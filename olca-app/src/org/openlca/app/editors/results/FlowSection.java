package org.openlca.app.editors.results;

import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ResultFlow;
import org.openlca.util.Categories;
import org.openlca.util.Strings;

record FlowSection(ResultEditor editor, boolean forInputs) {

	static FlowSection forInputs(ResultEditor editor) {
		return new FlowSection(editor, true);
	}

	static FlowSection forOutputs(ResultEditor editor) {
		return new FlowSection(editor, false);
	}

	void render(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk,
			M.InventoryResult + " - " + (forInputs ? M.Inputs : M.Outputs));
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);
		var table = Tables.createViewer(comp,
			M.Flow, M.Category, M.Amount, M.Unit, M.Location);
		table.setLabelProvider(new FlowLabel());
		Tables.bindColumnWidths(table, 0.2, 0.2, 0.2, 0.2, 0.2);

		var flows = editor.getModel().inventory.stream()
			.filter(flow -> flow.isInput == forInputs)
			.sorted((f1, f2) -> Strings.compare(
				Labels.name(f1.flow), Labels.name(f2.flow)))
			.collect(Collectors.toList());
		table.setInput(flows);
	}

	private static class FlowLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof ResultFlow r))
				return null;
			return switch (col) {
				case 0 -> Images.get(r.flow);
				case 4 -> r.location == null
					? null
					: Images.get(ModelType.LOCATION);
				default -> null;
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ResultFlow r))
				return null;
			return switch (col) {
				case 0 -> Labels.name(r.flow);
				case 1 -> {
					if (r.flow == null || r.flow.category == null)
						yield null;
					var path = Categories.path(r.flow.category);
					yield String.join("/", path);
				}
				case 2 -> Numbers.format(r.amount);
				case 3 -> Labels.name(r.unit);
				case 4 -> Labels.name(r.location);
				default -> null;
			};
		}
	}

}
