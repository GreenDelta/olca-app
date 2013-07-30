package org.openlca.app.io.ilcd.exporter;

import java.util.Collections;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.app.UI;
import org.openlca.app.resources.ImageType;
import org.openlca.core.application.actions.IExportAction;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for the export of a model component to ILCD.
 */
public class ILCDExportAction extends Action implements IExportAction {

	private Logger log = LoggerFactory.getLogger(getClass());
	private BaseDescriptor descriptor;

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.ILCD_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.ExportActionText;
	}

	@Override
	public void run() {
		if (descriptor == null) {
			log.error("Component or database is null");
			return;
		}
		ILCDExportWizard wizard = new ILCDExportWizard(
				descriptor.getModelType());
		wizard.setComponents(Collections.singletonList(descriptor));
		WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
		dialog.open();

	}

	@Override
	public void setDescriptor(BaseDescriptor descriptor) {
		this.descriptor = descriptor;
	}

}
