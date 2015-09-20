package org.openlca.app.editors.locations;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.db.Database;
import org.openlca.app.wizards.io.FileImportPage;
import org.openlca.io.MultiKmlImport;

public class KmlImportWizard extends Wizard implements IImportWizard {

	public static final String ID = "wizard.import.kml";
	private FileImportPage fileImportPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		addPage(fileImportPage = new FileImportPage(new String[] { "kml" },
				false));
	}

	@Override
	public boolean performFinish() {
		try {
			getContainer().run(true, false, (monitor) -> runImport(monitor));
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void runImport(IProgressMonitor monitor)
			throws InvocationTargetException {
		monitor.beginTask("#Importing KML data", IProgressMonitor.UNKNOWN);
		File file = fileImportPage.getFiles()[0];
		try (InputStream stream = new FileInputStream(file)) {
			MultiKmlImport parser = new MultiKmlImport(Database.get(), stream);
			parser.parseAndInsert();
			monitor.done();
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}
	}
}
