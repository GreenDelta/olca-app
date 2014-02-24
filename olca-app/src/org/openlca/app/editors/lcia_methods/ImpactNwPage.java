package org.openlca.app.editors.lcia_methods;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.NwSet;

class ImpactNwPage extends ModelPage<ImpactMethod> {

	private FormToolkit toolkit;
	private NwFactorViewer factorViewer;

	ImpactNwPage(ImpactMethodEditor editor) {
		super(editor, "ImpactNormalizationWeightingPage",
				Messages.NormalizationWeightingPageLabel);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.ImpactMethod
				+ ": " + getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);

		Section section = UI.section(body, toolkit,
				Messages.NormalizationWeightingSets);
		UI.gridData(section, true, true);
		Composite client = toolkit.createComposite(section);
		section.setClient(client);
		UI.gridLayout(client, 1);

		final SashForm sashForm = new SashForm(client, SWT.NONE);
		final GridData sashGD = new GridData(SWT.FILL, SWT.FILL, true, true);
		sashGD.widthHint = 400;
		sashForm.setLayoutData(sashGD);
		sashForm.setLayout(new GridLayout(2, false));

		NwSetViewer setViewer = new NwSetViewer(sashForm);
		setViewer.bindTo(section);
		setViewer
				.addSelectionChangedListener(new SetSelectionChangedListener());
		getBinding().on(getModel(), "nwSets", setViewer);

		factorViewer = new NwFactorViewer(sashForm, getModel());

		sashForm.setWeights(new int[] { 25, 75 });
		body.setFocus();

		setViewer.selectFirst();

		form.reflow(true);
	}

	private class SetSelectionChangedListener implements
			ISelectionChangedListener<NwSet> {

		@Override
		public void selectionChanged(NwSet selection) {
			getBinding().release(factorViewer);
			if (selection == null)
				factorViewer.setInput((NwSet) null);
			else {
				getBinding().on(selection, "factors", factorViewer);
			}
		}
	}

}
