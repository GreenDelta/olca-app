package org.openlca.app.editors.flows;

import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.ProcessWizard;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FlowInfoPage extends ModelPage<Flow> {

	FlowInfoPage(FlowEditor editor) {
		super(editor, "FlowInfoPage", M.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(this);
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		var infoSection = new InfoSection(getEditor());
		infoSection.render(body, tk);
		var useSection = new FlowUseSection(getModel(), Database.get());
		useSection.render(body, tk);
		additionalInfo(infoSection, body, tk);
		processButton(infoSection, tk);
		body.setFocus();
		form.reflow(true);
	}

	private void additionalInfo(InfoSection info, Composite body, FormToolkit tk) {
		var container = info.composite();
		checkBox(container, M.InfrastructureFlow, "infrastructureFlow");
		readOnly(container, M.FlowType, Images.get(getModel()), "flowType");

		var section = UI.section(body, tk, M.AdditionalInformation);
		var comp = UI.sectionClient(section, tk, 3);
		var casText = text(comp, M.CASNumber, "casNumber");
		var formulaText = text(comp, M.Formula, "formula");
		var synText = text(comp, M.Synonyms, "synonyms");
		modelLink(comp, M.Location, "location");

		Runnable pubChemFetch = () -> {
			var pub = App.exec(
				"Call PubChem API ...",
				() -> PubChemInfo.getFor(getModel())).orElse(null);
			if (pub == null)
				return;
			var update = pub.applyCasNumber(casText::setText)
				| pub.applyMolecularFormula(formulaText::setText)
				| pub.applySynonyms(synText::setText)
				| pub.applyProperties(
				() -> getEditor().emitEvent(ModelEditor.ON_ADDITIONAL_PROPS_CHANGED));
			if (update) {
				getEditor().setDirty();
			}
		};

		if (getEditor().isEditable()) {
			var pubChem = Actions.create(
				"Get from PubChem", Icon.PUBCHEM.descriptor(), pubChemFetch);
			Actions.bind(section, pubChem);
		}
	}

	private void processButton(InfoSection infoSection, FormToolkit toolkit) {
		Flow flow = getModel();
		if (flow.flowType != FlowType.PRODUCT_FLOW)
			return;
		var comp = infoSection.composite();
		UI.label(comp, toolkit, "");
		var button = UI.button(comp, toolkit, M.CreateProcess);
		button.setImage(Images.get(ModelType.PROCESS, Overlay.NEW));
		Controls.onSelect(button, e -> openProcessWizard());
	}

	private void openProcessWizard() {
		Flow flow = getModel();
		try {
			var w = PlatformUI.getWorkbench()
				.getNewWizardRegistry()
				.findWizard("wizards.new.process")
				.createWizard();
			if (!(w instanceof ProcessWizard wizard))
				return;
			wizard.setRefFlow(flow);
			var dialog = new WizardDialog(UI.shell(), wizard);
			if (dialog.open() == Window.OK) {
				Navigator.refresh(Navigator.findElement(ModelType.PROCESS));
			}
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to open process dialog from flow {}", flow, e);
		}
	}
}
