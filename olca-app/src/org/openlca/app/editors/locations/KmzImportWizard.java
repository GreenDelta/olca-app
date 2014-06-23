package org.openlca.app.editors.locations;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.db.Database;
import org.openlca.app.wizards.io.FileImportPage;
import org.openlca.io.refdata.GeoKmzImport;

public class KmzImportWizard extends Wizard implements IImportWizard {

	public static final String ID = "wizard.import.kml";
	private FileImportPage fileImportPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		addPage(fileImportPage = new FileImportPage(new String[] { "xml" },
				false));
	}

	@Override
	public boolean performFinish() {
		try {
			getContainer().run(true, false, (monitor) -> runImport(monitor));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	private void runImport(IProgressMonitor monitor) {
		monitor.beginTask("Importing KML data", IProgressMonitor.UNKNOWN);
		File file = fileImportPage.getFiles()[0];
		new GeoKmzImport(file, Database.get()).run();
		monitor.done();
	}

}
