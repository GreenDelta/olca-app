package org.openlca.app.editors.lcia_methods;

import java.util.Objects;

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
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.NwSet;

class ImpactNwPage extends ModelPage<ImpactMethod> {

	private FormToolkit toolkit;
	private NwFactorViewer factorViewer;
	private ImpactMethodEditor editor;
	private NwSetViewer setViewer;

	ImpactNwPage(ImpactMethodEditor editor) {
		super(editor, "ImpactNormalizationWeightingPage",
				Messages.NormalizationWeightingPageLabel);
		this.editor = editor;
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
		SashForm sashForm = createSash(client);
		setViewer = createNwSetViewer(section, sashForm);
		factorViewer = new NwFactorViewer(sashForm, editor);
		setViewer.selectFirst();
		body.setFocus();
		form.reflow(true);
		editor.onSaved(() -> updateInput());
	}

	private SashForm createSash(Composite client) {
		SashForm sash = new SashForm(client, SWT.NONE);
		GridData sashGD = new GridData(SWT.FILL, SWT.FILL, true, true);
		sashGD.widthHint = 400;
		sash.setLayoutData(sashGD);
		sash.setLayout(new GridLayout(2, false));
		sash.setWeights(new int[] { 25, 75 });
		return sash;
	}

	private NwSetViewer createNwSetViewer(Section section, SashForm sashForm) {
		NwSetViewer viewer = new NwSetViewer(sashForm, editor);
		viewer.bindTo(section);
		viewer.addSelectionChangedListener((selection) -> {
			factorViewer.setInput(selection);
		});
		viewer.setInput(getModel());
		return viewer;
	}

	private void updateInput() {
		setViewer.setInput(getModel());
		NwSet newNwSet = findNewNwSet();
		setViewer.select(newNwSet);
		factorViewer.setInput(newNwSet);
	}

	private NwSet findNewNwSet() {
		NwSet oldNwSet = setViewer.getSelected();
		if (oldNwSet == null)
			return null;
		for (NwSet newNwSet : getModel().getNwSets()) {
			if (Objects.equals(oldNwSet, newNwSet))
				return newNwSet;
		}
		return null;
	}
}
