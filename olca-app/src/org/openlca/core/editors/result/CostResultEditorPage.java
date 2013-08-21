package org.openlca.core.editors.result;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.core.editors.ContributionImage;

public class CostResultEditorPage extends FormPage {

	private static final String COST_CATEGORY = "Cost category";
	private static final String AMOUNT = "Amount";
	private static final String FIX_COSTS = "Fixed costs";

	private static final String[] PROPERTIES = { COST_CATEGORY, AMOUNT,
			FIX_COSTS };

	private CostResultEditorInput input;
	private List<CostResultItem> resultItems;

	public CostResultEditorPage(CostResultEditor editor,
			CostResultEditorInput input) {
		super(editor, "CostResultEditorPage", "Results");
		this.input = input;
		resultItems = CostResultItem.getItems(input.getResult());
		Collections.sort(resultItems);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		toolkit.decorateFormHeading(form.getForm());
		form.setText(input.getName());

		Composite body = UI.formBody(form, toolkit);

		TableViewer viewer = new TableViewer(body, SWT.BORDER
				| SWT.FULL_SELECTION | SWT.MULTI);
		Table table = viewer.getTable();
		toolkit.adapt(table);
		toolkit.paintBordersFor(table);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		viewer.setColumnProperties(PROPERTIES);
		for (String property : PROPERTIES) {
			TableColumn col = new TableColumn(table, SWT.NONE);
			col.setText(property);
		}
		Tables.bindColumnWidths(table, 0.5, 0.4, 0.1);

		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		UI.gridData(table, true, false).heightHint = 150;
		viewer.setInput(resultItems);

		Composite chartComposite = toolkit.createComposite(body);
		GridData data = UI.gridData(chartComposite, false, false);
		data.horizontalAlignment = SWT.CENTER;
		data.widthHint = 500;
		data.heightHint = 400;
		chartComposite.setLayout(new FillLayout());

		CostResultChart chart = new CostResultChart(resultItems);
		chart.render(chartComposite);

		form.reflow(true);
	}

	private class LabelProvider extends ColumnLabelProvider implements
			ITableLabelProvider {

		private ContributionImage image;

		public LabelProvider() {
			image = new ContributionImage(Display.getCurrent());
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof CostResultItem))
				return null;
			CostResultItem item = (CostResultItem) element;
			if (columnIndex == 0)
				return item.getCostCategory().getName();
			if (columnIndex == 1)
				return Double.toString(item.getAmount());
			if (columnIndex == 2)
				return item.getCostCategory().isFix() ? "Yes" : "No";
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof CostResultItem))
				return null;
			CostResultItem item = (CostResultItem) element;
			if (columnIndex == 0)
				return image.getForTable(item.getContribution());
			return null;
		}

		@Override
		public void dispose() {
			super.dispose();
			image.dispose();
		}

	}

}
