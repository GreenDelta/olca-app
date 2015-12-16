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

	private EntityCache cache = Cache.getEntityCache();
	private FormToolkit toolkit;
	private SimpleResultProvider<?> result;

	public TotalFlowResultPage(FormEditor editor, SimpleResultProvider<?> result) {
		super(editor, "InventoryPage", Messages.InventoryResults);
		this.result = result;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, Messages.InventoryResults);
		toolkit = mform.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		TableViewer inputViewer = createSectionAndViewer(body, true);
		TableViewer outputViewer = createSectionAndViewer(body, false);
		new TotalRequirementsSection(result).create(body, toolkit);
		form.reflow(true);
		Collection<FlowDescriptor> flows = result.getFlowDescriptors();
		inputViewer.setInput(flows);
		outputViewer.setInput(flows);
	}

	private TableViewer createSectionAndViewer(Composite parent, boolean input) {
		Section section = UI.section(parent, toolkit, input ? Messages.Inputs
				: Messages.Outputs);
		UI.gridData(section, true, true);
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		TableViewer viewer = Tables.createViewer(composite, new String[] {
				Messages.Flow, Messages.Category, Messages.SubCategory,
				Messages.Unit, Messages.Amount });
		Label label = new Label();
		viewer.setLabelProvider(label);
		viewer.setFilters(new ViewerFilter[] { new InputOutputFilter(input) });
		createColumnSorters(viewer, label);
		Tables.bindColumnWidths(viewer.getTable(), .4, .2, .2, .1, .1);
		Actions.bind(viewer, TableClipboard.onCopy(viewer));
		return viewer;
	}

	private void createColumnSorters(TableViewer viewer, Label label) {
		Viewers.sortByLabels(viewer, label, 0, 1, 2, 3);
		Function<FlowDescriptor, Double> amount = (f) -> {
			FlowResult r = result.getTotalFlowResult(f);
			return r == null ? 0 : r.value;
		};
		Viewers.sortByDouble(viewer, amount, 4);
	}

	private class Label extends BaseLabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col != 0)
				return null;
			if (!(obj instanceof FlowDescriptor))
				return null;
			FlowDescriptor flow = (FlowDescriptor) obj;
			return Images.getIcon(flow.getFlowType());
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof FlowDescriptor))
				return null;
			FlowDescriptor flow = (FlowDescriptor) obj;
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
				double v = result.getTotalFlowResult(flow).value;
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
			FlowIndex index = result.result.flowIndex;
			FlowDescriptor flow = (FlowDescriptor) element;
			return index.isInput(flow.getId()) == input;
		}
	}

}
