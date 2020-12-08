package org.openlca.app.wizards.io;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.io.HSCSim;
import org.openlca.io.maps.FlowMap;
import org.slf4j.LoggerFactory;

/**
 * A wizard for importing HSC Sim flow sheets as process data sets.
 */
public class HSCSimImportWizard extends Wizard implements IImportWizard {

	private FileImportPage page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import an HSC Sim Flow Sheet");
		setDefaultPageImageDescriptor(Icon.IMPORT.descriptor());
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page = new FileImportPage("json");
		page.withMappingFile = true;
		page.withMultiSelection = false;
		addPage(page);
	}

	@Override
	public boolean performFinish() {

		// check that we have an open database
		var db = Database.get();
		if (db == null) {
			MsgBox.error(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return false;
		}

		// we currently only support single file imports
		var files = page.getFiles();
		if (files == null || files.length == 0)
			return false;
		var file = files[0];
		if (file == null)
			return false;

		// run the import
		try {
			getContainer().run(true, true, m -> {
				m.beginTask(
						"Import " + file.getName(),
						IProgressMonitor.UNKNOWN);
				var flowMap = page.flowMap != null
						? page.flowMap
						: FlowMap.empty();
				HSCSim.importProcess(db, file, flowMap);
				m.done();
			});
			Navigator.refresh();
			return true;
		} catch (Exception e) {
			var log = LoggerFactory.getLogger(getClass());
			log.error("failed to import as HSC Sim file: " + file, e);
			MsgBox.error("Import failed",
					"An import error occurred: " + e.getMessage());
			return false;
		}
	}
}
