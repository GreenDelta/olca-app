package org.openlca.app.results.analysis.sankey;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.components.ResultTypeCombo;
import org.openlca.app.components.ResultTypeCombo.EventHandler;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.UI;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.FullResult;

public class SankeySelectionDialog extends FormDialog implements EventHandler {

	public double cutoff;
	public int maxCount;
	public Object selection;
	private final FullResult result;

	public SankeySelectionDialog(SankeyDiagram editor) {
		super(UI.shell());
		this.result = editor.result;
		this.selection = editor.selection;
		this.cutoff = editor.cutoff;
		this.maxCount = editor.maxCount;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		FormToolkit tk = mform.getToolkit();
		ScrolledForm form = UI.formHeader(mform,
				M.SettingsForTheSankeyDiagram);
		Composite body = UI.formBody(form, tk);
		UI.gridLayout(body, 2);
		ResultTypeCombo.on(result)
				.withEventHandler(this).withSelection(selection)
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
		spinner.setMaximum(result.techIndex.size());
		spinner.setDigits(0);
		spinner.setSelection(maxCount);
		spinner.addModifyListener(e -> {
			maxCount = spinner.getSelection();
		});
		tk.adapt(spinner);
		tk.createLabel(inner, "");
	}

	@Override
	public void flowSelected(IndexFlow flow) {
		this.selection = flow;
	}

	@Override
	public void impactCategorySelected(ImpactDescriptor impact) {
		this.selection = impact;
	}

	@Override
	public void costResultSelected(CostResultDescriptor cost) {
		this.selection = cost;
	}

}
