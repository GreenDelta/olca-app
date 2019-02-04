package org.openlca.app.editors.flows;

import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.LocationViewer;
import org.openlca.app.wizards.ProcessWizard;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FlowInfoPage extends ModelPage<Flow> {

	private FormToolkit toolkit;
	private ScrolledForm form;

	FlowInfoPage(FlowEditor editor) {
		super(editor, "FlowInfoPage", M.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(this);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		FlowUseSection useSection = new FlowUseSection(getModel(), Database.get());
		useSection.render(body, toolkit);
		createAdditionalInfo(infoSection, body);
		processButton(infoSection);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdditionalInfo(InfoSection infoSection, Composite body) {
		Composite container = infoSection.getContainer();
		checkBox(container, M.InfrastructureFlow, "infrastructureFlow");
		readOnly(container, M.FlowType, Images.get(getModel()), "flowType");
		Composite composite = UI.formSection(body, toolkit, M.AdditionalInformation, 3);
		text(composite, M.CASNumber, "casNumber");
		text(composite, M.Formula, "formula");
		text(composite, M.Synonyms, "synonyms");
		createLocationViewer(composite);
	}

	private void createLocationViewer(Composite composite) {
		new Label(composite, SWT.NONE).setText(M.Location);
		LocationViewer viewer = new LocationViewer(composite);
		viewer.setNullable(true);
		viewer.setInput(Database.get());
		getBinding().onModel(() -> getModel(), "location", viewer);
		new CommentControl(composite, getToolkit(), "location", getComments());
	}

	private void processButton(InfoSection infoSection) {
		Flow flow = getModel();
		if (flow.flowType != FlowType.PRODUCT_FLOW)
			return;
		Composite comp = infoSection.getContainer();
		toolkit.createLabel(comp, "");
		Button button = toolkit.createButton(comp, M.CreateProcess, SWT.NONE);
		button.setImage(Images.get(ModelType.PROCESS, Overlay.NEW));
		Controls.onSelect(button, e -> openProcessWizard());
	}

	private void openProcessWizard() {
		Flow flow = getModel();
		try {
			String wizardId = "wizards.new.process";
			IWorkbenchWizard w = PlatformUI.getWorkbench().getNewWizardRegistry().findWizard(wizardId).createWizard();
			if (!(w instanceof ProcessWizard))
				return;
			ProcessWizard wizard = (ProcessWizard) w;
			wizard.setRefFlow(flow);
			WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
			if (dialog.open() == Window.OK) {
				Navigator.refresh(Navigator.findElement(ModelType.PROCESS));
			}
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to open process dialog from flow " + flow, e);
		}
	}
}
