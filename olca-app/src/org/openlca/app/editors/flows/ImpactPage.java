package org.openlca.app.editors.flows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.ImpactCategoryDao;
import org.openlca.core.database.LocationDao;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.UnitDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.LocationDescriptor;
import org.openlca.util.Strings;

class ImpactPage extends ModelPage<Flow> {

	ImpactPage(FlowEditor editor) {
		super(editor, "FlowImpactPage", M.ImpactFactors);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(this);
		var tk = mform.getToolkit();
		var body = UI.formBody(form, tk);
		var table = Tables.createViewer(body,
				M.ImpactCategory,
				M.Category,
				M.Location,
				M.ImpactFactor,
				M.Unit);
		table.setLabelProvider(new Label());
		table.setInput(Factor.allOf(getModel()));
		Tables.bindColumnWidths(table, 0.2, 0.2, 0.2, 0.2, 0.2);
		var onOpen = Actions.onOpen(() -> {
			Factor f = Viewers.getFirstSelected(table);
			if (f != null) {
				App.open(f.impact);
			}
		});
		Actions.bind(table, onOpen);
		Tables.onDoubleClick(table, e -> onOpen.run());
		form.reflow(true);
	}

	private record Factor(
			ImpactDescriptor impact,
			Unit unit,
			LocationDescriptor location,
			double value
	) {

		static List<Factor> allOf(Flow flow) {
			var db = Database.get();
			if (flow == null || db == null)
				return Collections.emptyList();

			var factors = new ArrayList<Factor>();
			String sql = """
					select f_impact_category, f_unit, f_location, value
					  from tbl_impact_factors where f_flow =\s""" + flow.id;
			var impDao = new ImpactCategoryDao(db);
			var unitDao = new UnitDao(db);
			var locDao = new LocationDao(db);
			NativeSql.on(db).query(sql, r -> {
				factors.add(new Factor(
						impDao.getDescriptor(r.getLong(1)),
						unitDao.getForId(r.getLong(2)),
						locDao.getDescriptor(r.getLong(3)),
						r.getDouble("value")
				));
				return true;
			});

			factors.sort((f1, f2) -> {
				int c = Strings.compare(Labels.name(f1.impact), Labels.name(f2.impact));
				if (c != 0)
					return c;
				if (f1.location == null)
					return -1;
				if (f2.location == null)
					return 1;
				return Strings.compare(f1.location.code, f2.location.code);
			});

			return factors;
		}
	}

	private static class Label extends LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Factor f) || f.impact == null)
				return null;
			return switch (col) {
				case 0 -> Images.get(f.impact);
				case 1 -> f.impact.category != null
						? Images.getForCategory(ModelType.IMPACT_CATEGORY)
						: null;
				default -> null;
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Factor f) || f.impact == null)
				return null;
			return switch (col) {
				case 0 -> Labels.name(f.impact);
				case 1 -> Labels.category(f.impact);
				case 2 -> f.location != null ? f.location.code : null;
				case 3 -> Numbers.format(f.value);
				case 4 -> {
					var catUnit = f.impact.referenceUnit != null
							? f.impact.referenceUnit
							: "1";
					yield f.unit != null
							? catUnit + " / " + Labels.name(f.unit)
							: "?";
				}
				default -> null;
			};
		}
	}
}
