package org.openlca.app.navigation.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.io.ILCDExportWizard;
import org.openlca.core.model.ModelType;

/**
 * Action for the export of a model component to ILCD.
 */
public class ILCDExportAction extends ExportAction {

	public ILCDExportAction() {
		super(ModelType.ACTOR, ModelType.SOURCE, ModelType.UNIT_GROUP,
				ModelType.FLOW_PROPERTY, ModelType.FLOW, ModelType.PROCESS,
				ModelType.IMPACT_METHOD);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.ILCD_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.ILCDExportActionText;
	}

	@Override
	public void run() {
		ILCDExportWizard wizard = new ILCDExportWizard(getType());
		wizard.setComponents(getComponents());
		WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
		dialog.open();

	}

}
