package org.openlca.app.editors.lcia.geo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.FormulaCellEditor;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.core.database.FlowDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.io.CategoryPath;

class GeoFlowSection {

	private final GeoPage page;
	private TableViewer table;

	GeoFlowSection(GeoPage page) {
		this.page = page;
	}

	void drawOn(Composite body, FormToolkit tk) {

		// create the section
		Section section = UI.section(body, tk, "Flow bindings");
		UI.gridData(section, true, true);
		Composite comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);

		// create the table
		table = Tables.createViewer(comp,
				M.Flow,
				M.Category,
				M.Formula,
				M.Unit);
		table.setLabelProvider(new Label());
		Tables.bindColumnWidths(
				table, 0.25, 0.25, 0.25, 0.25);
		ModifySupport<GeoFlowBinding> ms = new ModifySupport<>(table);
		ms.bind(M.Formula, getFormulaEditor());

		// bind actions
		Action add = Actions.onAdd(() -> onAdd());
		Action remove = Actions.onRemove(() -> onRemove());
		Actions.bind(table, add, remove);
		Actions.bind(section, add, remove);
	}

	private void onRemove() {
		if (page.setup == null)
			return;
		List<GeoFlowBinding> bindings = Viewers.getAllSelected(table);
		if (bindings == null || bindings.isEmpty())
			return;
		page.setup.bindings.removeAll(bindings);
		table.setInput(page.setup.bindings);
	}

	private void onAdd() {
		if (page.setup == null)
			return;
		CategorizedDescriptor[] flows = ModelSelectionDialog
				.multiSelect(ModelType.FLOW);
		if (flows == null || flows.length == 0)
			return;
		FlowDao dao = new FlowDao(Database.get());
		for (CategorizedDescriptor d : flows) {
			boolean isPresent = false;
			for (GeoFlowBinding b : page.setup.bindings) {
				if (b.flow == null)
					continue;
				if (Objects.equals(b.flow.refId, d.refId)) {
					isPresent = true;
					break;
				}
			}
			if (isPresent)
				continue;
			Flow flow = dao.getForId(d.id);
			if (flow == null)
				continue;
			GeoFlowBinding b = new GeoFlowBinding(flow);
			page.setup.bindings.add(b);
		}
		table.setInput(page.setup.bindings);
	}

	void update() {
		if (page.setup == null)
			return;
		table.setInput(page.setup.bindings);
	}

	private FormulaCellEditor getFormulaEditor() {

		// TODO: it would be good to remove global
		// parameters from the auto-completion here
		// or, allow all parameters (also from the
		// indicator) here
		FormulaCellEditor editor = new FormulaCellEditor(table, () -> {
			if (page.setup == null)
				return Collections.emptyList();
			return page.setup.params.stream()
					.map(gp -> {
						Parameter p = new Parameter();
						p.name = gp.identifier;
						return p;
					})
					.collect(Collectors.toList());
		});

		editor.onEdited((obj, formula) -> {
			if (!(obj instanceof GeoFlowBinding))
				return;
			GeoFlowBinding binding = (GeoFlowBinding) obj;
			binding.formula = formula;
			table.refresh();
		});
		return editor;
	}

	private class Label extends LabelProvider implements ITableLabelProvider {

		@Override
	  public Image getColumnImage(Object obj, int col) {

	    if (!(obj instanceof GeoFlowBinding))
	      return null;
	    GeoFlowBinding b = (GeoFlowBinding) obj;
	    if (b.flow == null)
	      return null;

	    switch (col) {
	      case 0:
	        return Images.get(b.flow);
	      case 1:
	        return Images.get(b.flow.category);
	      case 2:
	        return Icon.FORMULA.get();
	      default:
	        return null;
	  }
		}

		@Override
		public String getColumnText(Object obj, int col) {

			if (!(obj instanceof GeoFlowBinding))
				return null;
			GeoFlowBinding b = (GeoFlowBinding) obj;
			if (b.flow == null)
				return null;

			switch (col) {
			case 0:
				return Labels.name(b.flow);
			case 1:
				return CategoryPath.getFull(b.flow.category);
			case 2:
				return b.formula;
			case 3:
				return Labels.name(b.flow.getReferenceUnit());
			default:
				return null;
			}
		}
	}
}
