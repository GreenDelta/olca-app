package org.openlca.app.editors.results;

import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Selections;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.FlowResult;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Result;
import org.openlca.util.Categories;
import org.openlca.util.Strings;

record FlowSection(ResultEditor editor, boolean forInputs) {

	static FlowSection forInputs(ResultEditor editor) {
		return new FlowSection(editor, true);
	}

	static FlowSection forOutputs(ResultEditor editor) {
		return new FlowSection(editor, false);
	}

	private Result result() {
		return editor.getModel();
	}

	void render(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk,
			M.InventoryResult + " - " + (forInputs ? M.Inputs : M.Outputs));
		UI.gridData(section, true, false);
		var comp = UI.sectionClient(section, tk, 1);
		var table = Tables.createViewer(comp,
			M.Flow, M.Category, M.Amount, M.Unit, M.Location);
		table.setLabelProvider(new FlowLabel(editor));
		Tables.bindColumnWidths(table, 0.2, 0.2, 0.2, 0.2, 0.2);

		var flows = editor.getModel().flowResults.stream()
			.filter(flow -> flow.isInput == forInputs)
			.sorted((f1, f2) -> Strings.compare(
				Labels.name(f1.flow), Labels.name(f2.flow)))
			.collect(Collectors.toList());
		table.setInput(flows);
		bindActions(section, table);
	}

	private void bindActions(Section section, TableViewer table) {

		var onAdd = Actions.onAdd(
			() -> new ModelSelector(ModelType.FLOW)
				.onOk(ModelSelector::first)
				.ifPresent(d -> {
					if (Util.addFlow(result(), d, forInputs)) {
						table.setInput(result().flowResults);
						editor.setDirty();
					}
				}));

		var onRemove = Actions.onRemove(() -> {
			FlowResult flow = Viewers.getFirstSelected(table);
			if (flow == null)
				return;
			var flows = result().flowResults;
			// TODO: check that this is not a linked flow in a system!
			flows.remove(flow);
			if (Objects.equals(result().referenceFlow, flow)) {
				result().referenceFlow = null;
			}
			table.setInput(flows);
			editor.setDirty();
		});

		var onOpen = Actions.onOpen(() -> {
			FlowResult flow = Viewers.getFirstSelected(table);
			if (flow != null) {
				App.open(flow.flow);
			}
		});
		Tables.onDoubleClick(table, $ -> onOpen.run());

		var refFlowAction = Actions.create(
			M.SetAsQuantitativeReference, Icon.FORMULA.descriptor(), () -> {
			FlowResult flow = Viewers.getFirstSelected(table);
			if (!isProviderFlow(flow))
				return;
			var result = editor.getModel();
			if (Objects.equals(result.referenceFlow, flow))
				return;
			editor.getModel().referenceFlow = flow;
			table.refresh();
			editor.setDirty();
		});

		// enable / disable the ref-flow-action
		table.addSelectionChangedListener(e -> {
			var selection = e.getStructuredSelection();
			if (selection == null || selection.size() != 1) {
				refFlowAction.setEnabled(false);
			}
			FlowResult flow = Selections.firstOf(selection);
			refFlowAction.setEnabled(isProviderFlow(flow));
		});

		Actions.bind(section, onAdd, onRemove);
		Actions.bind(table, onAdd, onRemove, onOpen,
			TableClipboard.onCopySelected(table), refFlowAction);

	}

	private boolean isProviderFlow(FlowResult flow) {
		if (flow == null
			|| flow.amount == 0
			|| flow.flow == null
			|| flow.flow.flowType == null)
			return false;
		return switch (flow.flow.flowType) {
			case ELEMENTARY_FLOW -> false;
			case PRODUCT_FLOW -> !flow.isInput;
			case WASTE_FLOW -> flow.isInput;
		};
	}

	private static class FlowLabel extends LabelProvider
		implements ITableLabelProvider, ITableFontProvider {

		private final ResultEditor editor;

		FlowLabel(ResultEditor editor) {
			this.editor = editor;
		}

		@Override
		public Font getFont(Object obj, int col) {
			if (!(obj instanceof FlowResult r))
				return null;
			var result = editor.getModel();
			return Objects.equals(result.referenceFlow, r)
				? UI.boldFont()
				: null;
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof FlowResult r))
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
			if (!(obj instanceof FlowResult r))
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
