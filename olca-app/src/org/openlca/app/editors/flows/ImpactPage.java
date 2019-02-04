package org.openlca.app.editors.flows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.EntityCache;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.NativeSql;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
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
				M.ImpactAssessmentMethod, M.ImpactCategory,
				M.ImpactFactor, M.Unit);
		table.setLabelProvider(new Label());
		table.setInput(loadFactors());
		Tables.bindColumnWidths(table, 0.3, 0.3, 0.2, 0.2);

		Action onOpen = Actions.onOpen(() -> {
			Factor f = Viewers.getFirstSelected(table);
			if (f != null) {
				App.openEditor(f.method);
			}
		});
		Actions.bind(table, onOpen);
		Tables.onDoubleClick(table, e -> onOpen.run());
		form.reflow(true);
	}

	private List<Factor> loadFactors() {
		IDatabase db = Database.get();
		Map<ImpactCategoryDescriptor, ImpactMethodDescriptor> rmap = new HashMap<>();
		ImpactMethodDao mdao = new ImpactMethodDao(db);
		mdao.getDescriptors().forEach(m -> {
			mdao.getCategoryDescriptors(m.id).forEach(c -> {
				rmap.put(c, m);
			});
		});
		String sql = "select f_impact_category, f_unit,"
				+ " value from tbl_impact_factors"
				+ " where f_flow = " + getModel().id;
		EntityCache ecache = Cache.getEntityCache();
		List<Factor> factors = new ArrayList<>();
		try {
			NativeSql.on(db).query(sql, r -> {
				Factor f = new Factor();
				f.impact = ecache.get(
						ImpactCategoryDescriptor.class, r.getLong(1));
				if (f.impact == null)
					return true;
				Unit unit = ecache.get(Unit.class, r.getLong(2));
				if (unit != null) {
					f.unit = unit.name;
				}
				f.value = r.getDouble(3);
				ImpactMethodDescriptor m = rmap.get(f.impact);
				if (m == null)
					return true;
				f.method = m;
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
					Labels.getDisplayName(f1.method),
					Labels.getDisplayName(f2.method));
			if (c != 0)
				return c;
			return Strings.compare(
					Labels.getDisplayName(f1.impact),
					Labels.getDisplayName(f2.impact));
		});
		ImpactMethodDescriptor m = null;
		for (Factor f : factors) {
			if (!Objects.equals(f.method, m)) {
				f.displayMethod = true;
				m = f.method;
			}
		}
		return factors;
	}

	private class Factor {
		ImpactMethodDescriptor method;
		ImpactCategoryDescriptor impact;
		double value;
		String unit;
		boolean displayMethod;
	}

	private class Label extends LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col != 0 || !(obj instanceof Factor))
				return null;
			Factor f = (Factor) obj;
			if (!f.displayMethod)
				return null;
			return Images.get(f.method);
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Factor))
				return null;
			Factor f = (Factor) obj;
			if (col == 0 && !f.displayMethod)
				return null;
			switch (col) {
			case 0:
				return Labels.getDisplayName(f.method);
			case 1:
				return Labels.getDisplayName(f.impact);
			case 2:
				return Double.toString(f.value);
			case 3:
				String catUnit = f.impact.referenceUnit;
				if (catUnit == null)
					return null;
				if (f.unit == null)
					return catUnit;
				return catUnit + " / " + f.unit;
			default:
				return null;
			}
		}
	}
}
