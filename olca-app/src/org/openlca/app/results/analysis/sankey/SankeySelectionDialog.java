package org.openlca.app.results.analysis.sankey;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ResultItemSelector;
import org.openlca.app.components.ResultItemSelector.SelectionHandler;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.UI;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.ResultItemView;

public class SankeySelectionDialog extends FormDialog implements SelectionHandler {

	private final ResultItemView resultItems;
	public double cutoff;
	public int maxCount;
	public Object selection;

	public SankeySelectionDialog(SankeyDiagram editor) {
		super(UI.shell());
		this.resultItems = editor.resultItems;
		this.selection = editor.selection;
		this.cutoff = editor.cutoff;
		this.maxCount = editor.maxCount;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var tk = mform.getToolkit();
		var form = UI.formHeader(mform, M.SettingsForTheSankeyDiagram);
		var body = UI.formBody(form, tk);
		UI.gridLayout(body, 2);
		ResultItemSelector.on(resultItems)
				.withSelectionHandler(this)
				.withSelection(selection)
				.create(body, tk);
		createCutoffSpinner(tk, body);
		createCountSpinner(tk, body);
	}

	private void createCutoffSpinner(FormToolkit tk, Composite comp) {
		tk.createLabel(comp, "Min. contribution share");
		var inner = tk.createComposite(comp);
		UI.gridLayout(inner, 2, 10, 0);
		var spinner = new Spinner(inner, SWT.BORDER);
		spinner.setIncrement(100);
		spinner.setMinimum(0);
		spinner.setMaximum(100000);
		spinner.setDigits(3);
		spinner.setSelection((int) (cutoff * 100000));
		spinner.addModifyListener(e -> {
			cutoff = spinner.getSelection() / 100000d;
		});
		tk.adapt(spinner);
		tk.createLabel(inner, "%");
	}

	private void createCountSpinner(FormToolkit tk, Composite comp) {
		tk.createLabel(comp, "Max. number of processes");
		var inner = tk.createComposite(comp);
		UI.gridLayout(inner, 2, 10, 0);
		var spinner = new Spinner(inner, SWT.BORDER);
		spinner.setIncrement(10);
		spinner.setMinimum(1);
		spinner.setMaximum(resultItems.techFlows().size());
		spinner.setDigits(0);
		spinner.setSelection(maxCount);
		spinner.addModifyListener(e -> {
			maxCount = spinner.getSelection();
		});
		tk.adapt(spinner);
		tk.createLabel(inner, "");
	}

	@Override
	public void onFlowSelected(EnviFlow flow) {
		this.selection = flow;
	}

	@Override
	public void onImpactSelected(ImpactDescriptor impact) {
		this.selection = impact;
	}

	@Override
	public void onCostsSelected(CostResultDescriptor cost) {
		this.selection = cost;
	}

}
