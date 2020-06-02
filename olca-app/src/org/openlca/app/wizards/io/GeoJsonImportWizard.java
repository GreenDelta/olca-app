package org.openlca.app.wizards.io;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.MsgBox;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.geo.GeoJsonImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoJsonImportWizard extends Wizard implements IImportWizard {

	private FileImportPage filePage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		filePage = new FileImportPage("json", "geojson");
		addPage(filePage);
	}

	@Override
	public boolean performFinish() {
		File[] files = filePage.getFiles();
		if (files == null || files.length == 0)
			return false;

		IDatabase db = Database.get();
		if (db == null) {
			MsgBox.error(
					M.NoDatabaseOpened,
					M.NeedOpenDatabase);
			return true;
		}

		try {
			getContainer().run(true, false, m -> {
				m.beginTask("Import geometries from GeoJSON...",
						IProgressMonitor.UNKNOWN);
				for (File f : files) {
					new GeoJsonImport(f, db).run();
				}
				m.done();
			});
			Navigator.refresh(
					Navigator.findElement(ModelType.LOCATION));
			return true;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Failed to import GeoJSON file", e);
			return true;
		}

	}
}
