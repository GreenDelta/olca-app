package org.openlca.app.wizards.io;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wizards for the import of data from an openLCA database to another openLCA
 * database.
 */
public class DbImportWizard extends Wizard implements IImportWizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	public void init(IWorkbench iWorkbench, IStructuredSelection
			iStructuredSelection) {
		setWindowTitle(Messages.DatabaseImport);
		setDefaultPageImageDescriptor(ImageType.IMPORT_ZIP_WIZARD
				.getDescriptor());
	}

	@Override
	public boolean performFinish() {
		return false;
	}

	@Override
	public void addPages() {
		addPage(new DbImportPage());
	}
}
