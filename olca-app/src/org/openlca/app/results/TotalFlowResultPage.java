package org.openlca.app.results;

import java.util.Collection;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.EntityCache;
import org.openlca.core.matrix.FlowIndex;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.results.FlowResult;
import org.openlca.core.results.SimpleResultProvider;

/**
 * Shows the total inventory result of a quick calculation, analysis result,
 * etc.
 */
public class TotalFlowResultPage extends FormPage {

	private final String FLOW = Messages.Flow;
	private final String CATEGORY = Messages.Category;
	private final String SUB_CATEGORY = Messages.SubCategory;
	private final String UNIT = Messages.Unit;
	private final String AMOUNT = Messages.Amount;

	private EntityCache cache = Cache.getEntityCache();
	private FormToolkit toolkit;
	private SimpleResultProvider<?> resultProvider;

	public TotalFlowResultPage(FormEditor editor,
			SimpleResultProvider<?> resultProvider) {
		super(editor, "InventoryResultPage", Messages.InventoryResults);
		this.resultProvider = resultProvider;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm,
				Messages.InventoryResults);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		TableViewer inputViewer = createSectionAndViewer(body, true);
		TableViewer outputViewer = createSectionAndViewer(body, false);
		form.reflow(true);
		Collection<FlowDescriptor> flows = resultProvider.getFlowDescriptors();
		inputViewer.setInput(flows);
		outputViewer.setInput(flows);
	}

	private TableViewer createSectionAndViewer(Composite parent,
			boolean input) {
		Section section = UI.section(parent, toolkit, input ? Messages.Inputs
				: Messages.Outputs);
		UI.gridData(section, true, true);
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		UI.gridLayout(composite, 1);
		TableViewer viewer = Tables.createViewer(composite, new String[] {
				FLOW, CATEGORY, SUB_CATEGORY, UNIT, AMOUNT });
		LabelProvider labelProvider = new LabelProvider();
		viewer.setLabelProvider(labelProvider);
		viewer.setFilters(new ViewerFilter[] { new InputOutputFilter(input) });
		createColumnSorters(viewer, labelProvider);
		Tables.bindColumnWidths(viewer.getTable(), 0.40, 0.20, 0.20, 0.08,
				0.10);
		Actions.bind(viewer, TableClipboard.onCopy(viewer));
		return viewer;
	}

	private void createColumnSorters(TableViewer viewer, LabelProvider label) {
		Viewers.sortByLabels(viewer, label, 0, 1, 2, 3);
		Function<FlowDescriptor, Double> amount = (f) -> {
			FlowResult r = resultProvider.getTotalFlowResult(f);
			return r == null ? 0 : r.value;
		};
		Viewers.sortByDouble(viewer, amount, 4);
	}

	private class LabelProvider extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			if (col != 0)
				return null;
			if (!(element instanceof FlowDescriptor))
				return null;
			FlowDescriptor flow = (FlowDescriptor) element;
			return Images.getIcon(flow.getFlowType());
		}

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof FlowDescriptor))
				return null;
			FlowDescriptor flow = (FlowDescriptor) element;
			Pair<String, String> category = Labels.getFlowCategory(flow, cache);
			switch (col) {
			case 0:
				return Labels.getDisplayName(flow);
			case 1:
				return category.getLeft();
			case 2:
				return category.getRight();
			case 3:
				return Labels.getRefUnit(flow, cache);
			case 4:
				double v = resultProvider.getTotalFlowResult(flow).value;
				return Numbers.format(v);
			default:
				return null;
			}
		}
	}

	private class InputOutputFilter extends ViewerFilter {

		private boolean input;

		private InputOutputFilter(boolean input) {
			this.input = input;
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if (!(element instanceof FlowDescriptor))
				return false;
			FlowIndex index = resultProvider.result.flowIndex;
			FlowDescriptor flow = (FlowDescriptor) element;
			return index.isInput(flow.getId()) == input;
		}
	}

}
