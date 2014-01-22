package org.openlca.app.wizards.io;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.resources.ImageType;
import org.openlca.core.database.IDatabase;
import org.openlca.io.olca.DatabaseImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wizards for the import of data from an openLCA database to another openLCA
 * database.
 */
public class DbImportWizard extends Wizard implements IImportWizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private DbImportPage page;

	@Override
	public void init(IWorkbench iWorkbench,
			IStructuredSelection iStructuredSelection) {
		setWindowTitle(Messages.DatabaseImport);
		setDefaultPageImageDescriptor(ImageType.IMPORT_ZIP_WIZARD
				.getDescriptor());
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		if (Database.get() == null) {
			org.openlca.app.util.Error.showBox("No database activated",
					"You need to activate a target database of the import.");
			return true;
		}
		DbImportPage.ImportConfig config = page.getConfig();
		try {
			getContainer().run(true, true, new ImportDispatch(config));
			Navigator.refresh();
			return true;
		} catch (Exception e) {
			log.error("database import failed", e);
			return false;
		}
	}

	@Override
	public void addPages() {
		page = new DbImportPage();
		addPage(page);
	}

	private class ImportDispatch implements IRunnableWithProgress {

		private DbImportPage.ImportConfig importConfig;

		ImportDispatch(DbImportPage.ImportConfig importConfig) {
			this.importConfig = importConfig;
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, OperationCanceledException {
			try {
				monitor.beginTask("Import database", IProgressMonitor.UNKNOWN);
				monitor.subTask("Open source database...");
				IDatabase source = connectToSource();
				monitor.subTask("Import data...");
				DatabaseImport dbImport = new DatabaseImport(source,
						Database.get());
				dbImport.run();
				monitor.subTask("Close source database...");
				closeDatabase(source);
				monitor.done();
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			}
		}

		private IDatabase connectToSource() throws Exception {
			if (importConfig.getMode() == importConfig.EXISTING_MODE)
				return importConfig.getDatabaseConfiguration().createInstance();
			// TODO: extract zolca-file in temporary folder and create
			// connection
			return null;
		}

		private void closeDatabase(IDatabase source) throws Exception {
			source.close();
			// TODO: delete files for file-based import
		}
	}
}
