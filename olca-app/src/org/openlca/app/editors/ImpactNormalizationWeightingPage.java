package org.openlca.app.editors;

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
import org.openlca.app.util.UI;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.table.NormalizationWeightingFactorViewer;
import org.openlca.app.viewers.table.NormalizationWeightingSetViewer;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.NormalizationWeightingSet;

class ImpactNormalizationWeightingPage extends ModelPage<ImpactMethod> {

	private FormToolkit toolkit;
	private NormalizationWeightingFactorViewer factorViewer;

	ImpactNormalizationWeightingPage(ImpactMethodEditor editor) {
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

		NormalizationWeightingSetViewer setViewer = new NormalizationWeightingSetViewer(
				sashForm);
		setViewer.bindTo(section);
		setViewer
				.addSelectionChangedListener(new SetSelectionChangedListener());
		getBinding().on(getModel(), "normalizationWeightingSets", setViewer);

		factorViewer = new NormalizationWeightingFactorViewer(sashForm,
				getModel());

		sashForm.setWeights(new int[] { 25, 75 });
		body.setFocus();
		form.reflow(true);
	}

	private class SetSelectionChangedListener implements
			ISelectionChangedListener<NormalizationWeightingSet> {

		@Override
		public void selectionChanged(NormalizationWeightingSet selection) {
			getBinding().release(factorViewer);
			if (selection == null)
				factorViewer.setInput(null);
			else {
				getBinding().on(selection, "normalizationWeightingFactors",
						factorViewer);
			}
		}
	}

}
