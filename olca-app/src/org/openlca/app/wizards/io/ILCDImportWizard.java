package org.openlca.app.wizards.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.Messages;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.resources.ImageType;
import org.openlca.core.database.IDatabase;
import org.openlca.io.ilcd.ILCDImport;

/**
 * Import wizard for the import of a set of ILCD files.
 */
public class ILCDImportWizard extends Wizard implements IImportWizard {

	private FileImportPage importPage;
	private IDatabase database;

	public ILCDImportWizard() {
		setNeedsProgressMonitor(true);
	}

	public ILCDImportWizard(IDatabase database) {
		setNeedsProgressMonitor(true);
		this.database = database;
	}

	@Override
	public void addPages() {
		importPage = new FileImportPage(new String[] { "zip" }, false);
		addPage(importPage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(Messages.ILCDImportWizard_WindowTitle);
		setDefaultPageImageDescriptor(ImageType.IMPORT_ZIP_WIZARD
				.getDescriptor());
	}

	@Override
	public boolean performFinish() {
		final File zip = getZip();
		if (zip == null)
			return false;
		try {
			doRun(zip);
			return true;
		} catch (final Exception e) {
			return false;
		} finally {
			Navigator.refresh();
		}
	}

	private File getZip() {
		File[] files = importPage.getFiles();
		if (files.length > 0)
			return files[0];
		return null;
	}

	private void doRun(final File zip) throws Exception {
		getContainer().run(true, true, new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				monitor.beginTask("Import: ", IProgressMonitor.UNKNOWN);
				ImportHandler handler = new ImportHandler(monitor);
				ILCDImport iImport = new ILCDImport(zip, database);
				handler.run(iImport);
			}
		});
	}

}
