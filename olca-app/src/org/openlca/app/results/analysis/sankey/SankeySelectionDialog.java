package org.openlca.app.results.analysis.sankey;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.components.ResultTypeSelection;
import org.openlca.app.components.ResultTypeSelection.EventHandler;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.UI;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.FullResult;

public class SankeySelectionDialog extends FormDialog implements EventHandler {

	public double cutoff = 0.1;
	public Object selection;
	private final FullResult result;

	public SankeySelectionDialog(FullResult result) {
		super(UI.shell());
		this.result = result;
	}

	@Override
	protected void createFormContent(final IManagedForm mform) {
		FormToolkit toolkit = mform.getToolkit();
		ScrolledForm form = UI.formHeader(mform,
				M.SettingsForTheSankeyDiagram);
		Composite body = UI.formBody(form, toolkit);
		UI.gridLayout(body, 2);
		ResultTypeSelection.on(result)
				.withEventHandler(this).withSelection(selection)
				.create(body, toolkit);
		createCutoffSpinner(toolkit, body);
	}

	private void createCutoffSpinner(FormToolkit toolkit, Composite composite) {
		toolkit.createLabel(composite, M.Cutoff);
		Spinner spinner = new Spinner(composite, SWT.BORDER);
		spinner.setIncrement(100);
		spinner.setMinimum(0);
		spinner.setMaximum(100000);
		spinner.setDigits(3);
		spinner.setSelection((int) (cutoff * 100000));
		spinner.addModifyListener(e -> {
			cutoff = spinner.getSelection() / 100000d;
		});
		toolkit.adapt(spinner);
	}

	@Override
	public void flowSelected(FlowDescriptor flow) {
		this.selection = flow;
	}

	@Override
	public void impactCategorySelected(ImpactCategoryDescriptor impact) {
		this.selection = impact;
	}

	@Override
	public void costResultSelected(CostResultDescriptor cost) {
		this.selection = cost;

	}

}
