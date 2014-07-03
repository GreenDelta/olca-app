package org.openlca.app.results.analysis.sankey;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.components.FlowImpactSelection;
import org.openlca.app.components.FlowImpactSelection.EventHandler;
import org.openlca.app.db.Cache;
import org.openlca.app.util.UI;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.FullResultProvider;

public class SankeySelectionDialog extends FormDialog implements EventHandler {

	private double cutoff = 0.1;
	private FullResultProvider result;
	private Object selection;

	public SankeySelectionDialog(FullResultProvider result) {
		super(UI.shell());
		this.result = result;
	}

	@Override
	protected void createFormContent(final IManagedForm mform) {
		FormToolkit toolkit = mform.getToolkit();
		ScrolledForm form = UI.formHeader(mform,
				Messages.SettingsForTheSankeyDiagram);
		Composite body = UI.formBody(form, toolkit);
		FlowImpactSelection.on(result, Cache.getEntityCache())
				.withEventHandler(this).withSelection(selection)
				.create(body, toolkit);
		createCutoffSpinner(toolkit, body);
	}

	private void createCutoffSpinner(FormToolkit toolkit, Composite body) {
		Composite composite = UI.formComposite(body, toolkit);
		toolkit.createLabel(composite, Messages.CutOffForFirstLayerIn);
		final Spinner cutoffSpinner = new Spinner(composite, SWT.BORDER);
		cutoffSpinner.setIncrement(100);
		cutoffSpinner.setMinimum(0);
		cutoffSpinner.setMaximum(100000);
		cutoffSpinner.setDigits(3);
		cutoffSpinner.setSelection((int) (cutoff * 100000));
		cutoffSpinner.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				cutoff = cutoffSpinner.getSelection() / 100000d;
			}
		});
		toolkit.adapt(cutoffSpinner);
	}

	public double getCutoff() {
		return cutoff;
	}

	public Object getSelection() {
		return selection;
	}

	public void setCutoff(double cutoff) {
		this.cutoff = cutoff;
	}

	public void setSelection(Object selection) {
		this.selection = selection;
	}

	@Override
	public void flowSelected(FlowDescriptor flow) {
		this.selection = flow;
	}

	@Override
	public void impactCategorySelected(ImpactCategoryDescriptor impactCategory) {
		this.selection = impactCategory;
	}

}
