package org.openlca.app.results;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.CostResults;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.core.results.SimpleResultProvider;

public class TotalCostResultPage extends FormPage {

	private SimpleResultProvider<?> result;

	public TotalCostResultPage(FormEditor editor, SimpleResultProvider<?> result) {
		super(editor, "CostResultPage", "#Costs");
		this.result = result;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, "#Costs/Added values");
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		TableViewer table = createTable(body, tk);
		form.reflow(true);
		table.setInput(CostResults.getDescriptors(result));
	}

	private TableViewer createTable(Composite body, FormToolkit tk) {
		Section section = UI.section(body, tk, "#Costs/Added values");
		UI.gridData(section, true, true);
		Composite composite = tk.createComposite(section);
		section.setClient(composite);
		UI.gridLayout(composite, 1);
		String[] columns = { Messages.CostCategory, Messages.Result, "#Currency" };
		TableViewer table = Tables.createViewer(composite, columns);
		Label label = new Label();
		table.setLabelProvider(label);
		Tables.bindColumnWidths(table, 0.50, 0.30, 0.2);
		Actions.bind(table, TableClipboard.onCopy(table));
		return table;
	}

	private class Label extends BaseLabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			if (col != 0)
				return null;
			return ImageType.COST_CALC_ICON.get();
		}

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof CostResultDescriptor))
				return null;
			CostResultDescriptor d = (CostResultDescriptor) element;
			switch (col) {
			case 0:
				return d.getName();
			case 1:
				double val = result.getTotalCostResult();
				val = d.forAddedValue ? -val : val;
				return Numbers.format(val);
			case 2:
				return Labels.getReferenceCurrencyCode();
			default:
				return null;
			}
		}
	}
}
