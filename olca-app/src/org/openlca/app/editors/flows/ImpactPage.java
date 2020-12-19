package org.openlca.app.editors.flows;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.EntityCache;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.LocationDescriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ImpactPage extends ModelPage<Flow> {

	ImpactPage(FlowEditor editor) {
		super(editor, "FlowImpactPage", M.ImpactFactors);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(this);
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		TableViewer table = Tables.createViewer(body,
				M.ImpactCategory, M.Location,
				M.ImpactFactor, M.Unit);
		table.setLabelProvider(new Label());
		table.setInput(loadFactors());
		Tables.bindColumnWidths(table, 0.4, 0.2, 0.2, 0.2);

		Action onOpen = Actions.onOpen(() -> {
			Factor f = Viewers.getFirstSelected(table);
			if (f != null) {
				App.open(f.impact);
			}
		});
		Actions.bind(table, onOpen);
		Tables.onDoubleClick(table, e -> onOpen.run());
		form.reflow(true);
	}

	private List<Factor> loadFactors() {
		IDatabase db = Database.get();
		String sql = "select f_impact_category, f_unit, f_location, "
				+ "value from tbl_impact_factors where f_flow = "
				+ getModel().id;
		EntityCache ecache = Cache.getEntityCache();
		List<Factor> factors = new ArrayList<>();
		try {
			NativeSql.on(db).query(sql, r -> {
				Factor f = new Factor();
				f.impact = ecache.get(
						ImpactDescriptor.class, r.getLong(1));
				if (f.impact == null)
					return true;
				Unit unit = ecache.get(Unit.class, r.getLong(2));
				if (unit != null) {
					f.unit = unit.name;
				}
				long locID = r.getLong(3);
				if (!r.wasNull()) {
					f.location = ecache.get(LocationDescriptor.class, locID);
				}
				f.value = r.getDouble(4);
				factors.add(f);
				return true;
			});
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Failed to load LCIA factors", e);
		}

		// sort and set display flags
		factors.sort((f1, f2) -> {
			int c = Strings.compare(
					Labels.name(f1.impact),
					Labels.name(f2.impact));
			if (c != 0)
				return c;
			if (f1.location == null)
				return -1;
			if (f2.location == null)
				return 1;
			return Strings.compare(
					f1.location.code,
					f2.location.code);
		});
		return factors;
	}

	private static class Factor {
		ImpactDescriptor impact;
		LocationDescriptor location;
		double value;
		String unit;
	}

	private static class Label extends LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col != 0 || !(obj instanceof Factor))
				return null;
			Factor f = (Factor) obj;
			return Images.get(f.impact);
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Factor))
				return null;
			Factor f = (Factor) obj;
			switch (col) {
			case 0:
				return Labels.name(f.impact);
			case 1:
				return f.location != null
						? f.location.code
						: null;
			case 2:
				return Numbers.format(f.value);
			case 3:
				String catUnit = f.impact.referenceUnit;
				if (catUnit == null) {
					catUnit = "1";
				}
				if (f.unit == null)
					return "?";
				return catUnit + " / " + f.unit;
			default:
				return null;
			}
		}
	}
}
