package org.openlca.app.editors.flows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openlca.app.db.Libraries;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.Labels;
import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactCategoryDao;
import org.openlca.core.database.LocationDao;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.UnitDao;
import org.openlca.core.library.LibMatrix;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.LocationDescriptor;

record UsedImpactFactor(
	ImpactDescriptor impact,
	Unit unit,
	LocationDescriptor location,
	double value
) {

	static List<UsedImpactFactor> allOf(Flow flow, IDatabase db) {
		return flow == null || db == null
			? Collections.emptyList()
			: new Scan(flow, db).collect();
	}

	private record Scan(Flow flow, IDatabase db) {

		List<UsedImpactFactor> collect() {
			var factors = new ArrayList<UsedImpactFactor>();
			addFromDatabase(factors);
			addFromLibraries(factors);

			factors.sort((fi, fj) -> {
				int c = Strings.compareIgnoreCase(
					Labels.name(fi.impact), Labels.name(fj.impact));
				if (c != 0 || Objects.equals(fi.location, fj.location))
					return c;
				if (fi.location == null)
					return -1;
				if (fj.location == null)
					return 1;
				return Strings.compareIgnoreCase(fi.location.code, fj.location.code);
			});
			return factors;
		}

		private void addFromDatabase(List<UsedImpactFactor> factors) {
			var sql = """
				select f_impact_category, f_unit, f_location, value
					from tbl_impact_factors where f_flow =\s""" + flow.id;
			var impDao = new ImpactCategoryDao(db);
			var unitDao = new UnitDao(db);
			var locDao = new LocationDao(db);
			NativeSql.on(db).query(sql, r -> {
				factors.add(new UsedImpactFactor(
					impDao.getDescriptor(r.getLong(1)),
					unitDao.getForId(r.getLong(2)),
					locDao.getDescriptor(r.getLong(3)),
					r.getDouble("value")
				));
				return true;
			});
		}

		/// Collects the characterization factors of the given flow from the
		/// matrices of the mounted libraries. When an impact category is in a
		/// library, its characterization factors are stored in the matrix C of
		/// that library and are not contained in the database.
		private void addFromLibraries(List<UsedImpactFactor> factors) {
			var libDir = Workspace.getLibraryDir();
			for (var libId : db.getLibraries()) {
				var lib = libDir.getLibrary(libId).orElse(null);
				if (lib == null || !lib.hasMatrix(LibMatrix.C))
					continue;
				var reader = Libraries.readerOf(lib).orElse(null);
				if (reader == null)
					continue;

				// find the columns of the flow in the library index; skip the
				// library when it does not contain the flow
				var enviIndex = reader.enviIndex();
				if (enviIndex == null)
					continue;
				var cols = new ArrayList<Integer>();
				for (int col = 0; col < enviIndex.size(); col++) {
					var enviFlow = enviIndex.at(col);
					if (enviFlow != null
						&& enviFlow.flow() != null
						&& Objects.equals(flow.refId, enviFlow.flow().refId)) {
						cols.add(col);
					}
				}
				if (cols.isEmpty())
					continue;

				var impactIndex = reader.impactIndex();
				var matrix = reader.matrixOf(LibMatrix.C);
				if (impactIndex == null || matrix == null)
					continue;

				// add the factors of those impact categories that have a
				// characterization factor for the flow
				for (int row = 0; row < impactIndex.size(); row++) {
					boolean hasFactor = false;
					for (int col : cols) {
						if (matrix.get(row, col) != 0) {
							hasFactor = true;
							break;
						}
					}
					if (!hasFactor)
						continue;
					var impact = impactIndex.at(row);
					for (var factor : reader.getImpactFactors(impact, db)) {
						if (factor.flow == null
							|| !Objects.equals(flow.refId, factor.flow.refId))
							continue;
						factors.add(new UsedImpactFactor(
							impact,
							factor.unit,
							factor.location != null
								? Descriptor.of(factor.location)
								: null,
							factor.value));
					}
				}
			}
		}
	}
}
