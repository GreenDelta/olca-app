package org.openlca.io.ui.ilcd.exporter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.core.application.actions.IExportAction;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for the export of a model component to ILCD.
 */
public class ILCDExportAction extends Action implements IExportAction {

	private Logger log = LoggerFactory.getLogger(getClass());
	private IModelComponent component;
	private IDatabase database;

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
		if (component == null || database == null) {
			log.error("Component or database is null");
			return;
		}
		ILCDExportWizard wizard = new ILCDExportWizard(-1); // type is not
															// relevant? -1
		wizard.setSingleExport(component, database);
		WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
		dialog.open();

	}

	@Override
	public void setComponent(final IModelComponent modelComponent) {
		this.component = modelComponent;
	}

	@Override
	public void setDatabase(final IDatabase database) {
		this.database = database;
	}

}
