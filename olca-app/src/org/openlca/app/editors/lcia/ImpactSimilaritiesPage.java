package org.openlca.app.editors.lcia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.ImpactCategoryDao;
import org.openlca.core.database.NativeSql;
import org.openlca.core.matrix.cache.ConversionTable;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.util.Strings;

class ImpactSimilaritiesPage extends ModelPage<ImpactCategory> {

	ImpactSimilaritiesPage(ImpactCategoryEditor editor) {
		super(editor, "Similarities", "Similarities");
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(this);
		var tk = mform.getToolkit();
		var body = UI.formBody(form, tk);
		var section = UI.section(body, tk,
			"Similarities to other impact categories");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);
		var table = Tables.createViewer(
			comp, M.Name, "Similarity");

	}

	private static class Item {
		final ImpactDescriptor impact;
		final double similarity;

		Item(ImpactDescriptor impact, double similarity) {
			this.impact = impact;
			this.similarity = similarity;
		}

		List<Item> listOf(ImpactCategory impact) {
			var db = Database.get();
			if (impact == null || db == null)
				return Collections.emptyList();

			// collect the flow IDs in an index
			var sql = "select f_flow from tbl_impact_factors";
			var flowIndex = new HashMap<Long, Integer>();
			NativeSql.on(db).query(sql, r -> {
				var flowID = r.getLong(1);
				flowIndex.computeIfAbsent(
					flowID, _key -> flowIndex.size());
				return true;
			});

			// collect the factors
			var conversions = ConversionTable.create(db);
			var factors = new HashMap<Long, double[]>();
			sql = "select f_impact_category, f_flow, " +
				"value, f_unit, f_flow_property_factor " +
				"from tbl_impact_factors";
			NativeSql.on(db).query(sql, r -> {
				var impactID = r.getLong(1);
				var flowID = r.getLong(2);
				var flowIdx = flowIndex.get(flowID);
				if (flowIdx == null)
					return true;
				var values = factors.computeIfAbsent(
					impactID, _key -> new double[flowIndex.size()]);

				var factor = r.getDouble(3);
				var unitF = conversions.getUnitFactor(r.getLong(4));
				factor = unitF == 0
					? factor
					: factor / unitF;
				var propF = conversions.getPropertyFactor(r.getLong(5));
				factor = propF == 0
					? factor
					: factor * propF;
				values[flowIdx] = factor;
				return true;
			});

			// calculate the items
			var items = new ArrayList<Item>();
			var baseline = factors.get(impact.id);
			for (var d : new ImpactCategoryDao(db).getDescriptors()) {
				if (d.id == impact.id)
					continue;
				var vec = factors.get(d.id);
				if (vec == null) {
					items.add(new Item(d, baseline == null ? 1 : 0));
					continue;
				}
				if (baseline == null) {
					items.add(new Item(d, 0));
					continue;
				}
				double sim = 0.0;
				double n = 0.0;
				for (int i = 0; i < vec.length; i++) {
					var v1 = baseline[i];
					var v2 = vec[i];
					if (v1 == 0 && v2 == 0)
						continue;
					n += 1;
					if (v1 == 0 || v2 == 0)
						continue;
					if (Math.abs(v1 - v2) < 1e-12) {
						sim += 1;
						continue;
					}
					var max = Math.max(v1, v2);
					var min = Math.min(v1, v2);
					sim += min / max;
				}

				if (n == 0) {
					items.add(new Item(d, 0));
				} else {
					items.add(new Item(d, sim / n));
				}
			}

			items.sort((i1, i2) -> {
				int c = -Double.compare(i1.similarity, i2.similarity);
				return c != 0
					? c
					: Strings.compare(
					Labels.name(i1.impact), Labels.name(i2.impact));
			});

			return items;
		}
	}


}
