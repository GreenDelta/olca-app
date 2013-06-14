package org.openlca.core.editors.analyze;

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
import org.openlca.core.application.Numbers;
import org.openlca.core.editors.model.FlowInfo;
import org.openlca.core.model.Flow;
import org.openlca.core.model.results.AnalysisResult;
import org.openlca.ui.UI;
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

	private AnalyzeEditor editor;
	private FormToolkit toolkit;
	private AnalysisResult result;

	public LCITotalPage(AnalyzeEditor editor, AnalysisResult result) {
		super(editor, LCITotalPage.class.getCanonicalName(), "LCI - Total");
		this.editor = editor;
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

		inputViewer.setInput(result.getFlowIndex().getFlows());
		outputViewer.setInput(result.getFlowIndex().getFlows());
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
		UI.bindColumnWidths(viewer.getTable(), COLUMN_WIDTHS);
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
			if (!(element instanceof Flow))
				return null;

			FlowInfo flow = editor.getFlowInfo((Flow) element);
			String columnLabel = COLUMN_LABELS.VALUES[columnIndex];

			switch (columnLabel) {
			case COLUMN_LABELS.FLOW:
				return flow.getName();
			case COLUMN_LABELS.CATEGORY:
				return flow.getCategory();
			case COLUMN_LABELS.SUBCATEGORY:
				return flow.getSubCategory();
			case COLUMN_LABELS.UNIT:
				return flow.getUnit();
			case COLUMN_LABELS.RESULT:
				return Numbers.format(result.getResult(result.getSetup()
						.getReferenceProcess(), (Flow) element));
			}

			return null;
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
			return result.getFlowIndex().isInput((Flow) element) == input;
		}

	}

	private class FlowViewerSorter extends ViewerSorter {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (!(e1 instanceof Flow)) {
				if (e2 != null)
					return -1;
				return 0;
			}
			if (!(e2 instanceof Flow))
				return 1;

			FlowInfo flow1 = editor.getFlowInfo((Flow) e1);
			FlowInfo flow2 = editor.getFlowInfo((Flow) e2);

			// safe compare flow names
			int flowNameCompare = Strings.compare(flow1.getName(),
					flow2.getName());
			if (flowNameCompare != 0)
				return flowNameCompare;

			int categoryCompare = Strings.compare(flow1.getCategory(),
					flow2.getCategory());
			if (categoryCompare != 0)
				return categoryCompare;

			int subcategoryCompare = Strings.compare(flow1.getSubCategory(),
					flow2.getSubCategory());
			if (subcategoryCompare != 0)
				return subcategoryCompare;
			return 0;
		}

	}

}
