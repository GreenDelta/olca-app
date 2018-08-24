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
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.wizards.io.FileImportPage;
import org.openlca.core.model.ModelType;
import org.openlca.geo.io.MultiKmlImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			Navigator.refresh(Navigator.findElement(ModelType.LOCATION));
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Failed to import KML", e);
			return false;
		}
		return true;
	}

	private void runImport(IProgressMonitor monitor)
			throws InvocationTargetException {
		monitor.beginTask(M.ImportingKMLData, IProgressMonitor.UNKNOWN);
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
