package org.openlca.app.wizards.io;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.openlca.geo.GeoJsonImport;

public class GeoJsonImportWizard extends Wizard implements IImportWizard {

	private Page page;
	private File initialFile;

	public GeoJsonImportWizard() {
		setWindowTitle(M.ImportGeoJson);
		setDefaultPageImageDescriptor(Icon.IMPORT_WIZARD.descriptor());
		setNeedsProgressMonitor(true);
	}

	public static void of(File file) {
		if (Database.isNoneActive()) {
			MsgBox.info(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		Wizards.forImport(
				"wizard.import.geojson",
				(GeoJsonImportWizard w) -> w.initialFile = file);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		if (Database.isNoneActive()) {
			addPage(new NoDatabaseErrorPage());
			return;
		}
		page = new Page(initialFile);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		if (page.json == null)
			return false;
		try {
			getContainer().run(true, false, m -> {
				m.beginTask(M.ImportGeometriesFromGeoJsonDots,
						IProgressMonitor.UNKNOWN);
				new GeoJsonImport(page.json, Database.get())
						.withMode(page.mode)
						.run();
				m.done();
			});
			Navigator.refresh(
					Navigator.findElement(ModelType.LOCATION));
			return true;
		} catch (Exception e) {
			ErrorReporter.on("Failed to import GeoJSON file", e);
			return true;
		}
	}

	private static class Page extends WizardPage {

		private final GeoJsonImport.Mode[] mods = {
				GeoJsonImport.Mode.NEW_ONLY,
				GeoJsonImport.Mode.UPDATE_ONLY,
				GeoJsonImport.Mode.NEW_AND_UPDATE
		};
		File json;
		GeoJsonImport.Mode mode = GeoJsonImport.Mode.NEW_ONLY;

		Page(File json) {
			super("GeoJsonImport.Page");
			setTitle(M.ImportGeographiesFromGeoJson);
			setDescription(M.SelectAGeoJsonFileAndAnImportMode);
			this.json = json;
			setPageComplete(json != null);
		}

		@Override
		public void createControl(Composite parent) {
			var body = UI.composite(parent);
			UI.gridLayout(body, 1).verticalSpacing = 0;

			// file selector
			var fileComp = UI.composite(body);
			UI.fillHorizontal(fileComp);
			UI.gridLayout(fileComp, 3).marginBottom = 0;
			FileSelector.on(file -> {
						json = file;
						setPageComplete(true);
					})
					.withTitle(M.SelectAGeoJsonFileDots)
					.withExtensions("*.geojson", "*.json")
					.withSelection(json)
					.render(fileComp);

			// import modes
			var groupComp = UI.composite(body);
			UI.gridLayout(groupComp, 1).marginTop = 0;
			UI.fillHorizontal(groupComp);
			var group = UI.group(groupComp);
			group.setText(M.ImportMode);
			UI.gridData(group, true, false);
			UI.gridLayout(group, 1);
			for (var m : mods) {
				var option = new Button(group, SWT.RADIO);
				option.setText(getText(m));
				option.setSelection(m == mode);
				Controls.onSelect(option, e -> mode = m);
			}
			setControl(body);
		}

		private String getText(GeoJsonImport.Mode mode) {
			return switch (mode) {
				case NEW_ONLY -> M.ImportNewLocations;
				case UPDATE_ONLY -> M.UpdateExistingLocations;
				case NEW_AND_UPDATE -> M.ImportUpdateLocations;
			};
		}
	}
}
