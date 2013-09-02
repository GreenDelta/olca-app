package org.openlca.core.editors.analyze;

import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.db.Database;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.core.database.Cache;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.results.AnalysisResult;
import org.openlca.util.Strings;

public class LCITotalPage extends FormPage {

	private interface COLUMN_LABELS {

		String FLOW = "Flow";
		String CATEGORY = "Category";
		String SUBCATEGORY = "Subcategory";
		String UNIT = "Unit";
		String RESULT = "Result";
		String[] VALUES = { FLOW, CATEGORY, SUBCATEGORY, UNIT, RESULT };

	}

	private static final double[] COLUMN_WIDTHS = { 0.40, 0.20, 0.20, 0.08,
			0.10 };

	private Cache cache = Database.getCache();
	private FormToolkit toolkit;
	private AnalysisResult result;

	public LCITotalPage(AnalyzeEditor editor, AnalysisResult result) {
		super(editor, LCITotalPage.class.getCanonicalName(), "LCI - Total");
		this.result = result;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		toolkit = managedForm.getToolkit();
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		form.setText("LCI - Total");
		toolkit.decorateFormHeading(form.getForm());

		Composite body = UI.formBody(form, toolkit);
		TableViewer inputViewer = createSectionAndViewer(body, true); // input
		TableViewer outputViewer = createSectionAndViewer(body, false); // output

		form.reflow(true);

		Set<FlowDescriptor> flows = result.getFlowResults().getFlows(cache);
		inputViewer.setInput(flows);
		outputViewer.setInput(flows);
	}

	private TableViewer createSectionAndViewer(Composite parent, boolean input) {
		Section section = UI.section(parent, toolkit, input ? "Inputs"
				: "Outputs");
		UI.gridData(section, true, true);
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		UI.gridLayout(composite, 1);

		TableViewer viewer = new TableViewer(composite);
		viewer.setLabelProvider(new LCILabelProvider());
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.getTable().setLinesVisible(true);
		viewer.getTable().setHeaderVisible(true);

		for (int i = 0; i < COLUMN_LABELS.VALUES.length; i++) {
			final TableColumn c = new TableColumn(viewer.getTable(), SWT.NULL);
			c.setText(COLUMN_LABELS.VALUES[i]);
		}
		viewer.setColumnProperties(COLUMN_LABELS.VALUES);
		viewer.setFilters(new ViewerFilter[] { new InputOutputFilter(input) });
		viewer.setSorter(new FlowViewerSorter());
		UI.gridData(viewer.getTable(), true, true);
		Tables.bindColumnWidths(viewer.getTable(), COLUMN_WIDTHS);
		return viewer;
	}

	private class LCILabelProvider extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof FlowDescriptor))
				return null;

			FlowDescriptor flow = (FlowDescriptor) element;
			Pair<String, String> category = Labels.getFlowCategory(flow, cache);
			String columnLabel = COLUMN_LABELS.VALUES[columnIndex];

			switch (columnLabel) {
			case COLUMN_LABELS.FLOW:
				return flow.getName();
			case COLUMN_LABELS.CATEGORY:
				return category.getLeft();
			case COLUMN_LABELS.SUBCATEGORY:
				return category.getRight();
			case COLUMN_LABELS.UNIT:
				return Labels.getRefUnit(flow, cache);
			case COLUMN_LABELS.RESULT:
				return Numbers.format(result.getFlowResults().getTotalResult(
						flow));
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
			FlowDescriptor flow = (FlowDescriptor) element;
			return result.getFlowIndex().isInput(flow.getId()) == input;
		}

	}

	private class FlowViewerSorter extends ViewerSorter {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (!(e1 instanceof FlowDescriptor))
				return 0;
			if (!(e2 instanceof FlowDescriptor))
				return 0;

			FlowDescriptor flow1 = (FlowDescriptor) e1;
			Pair<String, String> cat1 = Labels.getFlowCategory(flow1, cache);
			FlowDescriptor flow2 = (FlowDescriptor) e2;
			Pair<String, String> cat2 = Labels.getFlowCategory(flow2, cache);

			int c = Strings.compare(cat1.getLeft(), cat2.getLeft());
			if (c != 0)
				return c;
			c = Strings.compare(cat1.getRight(), cat2.getLeft());
			if (c != 0)
				return c;
			return Strings.compare(flow1.getName(), flow2.getName());
		}

	}

}
