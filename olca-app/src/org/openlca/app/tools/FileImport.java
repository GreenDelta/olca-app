package org.openlca.app.tools;

import java.io.File;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.devtools.SaveScriptDialog;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.db.DbRestoreAction;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.openepd.input.ImportDialog;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.io.DbImportWizard;
import org.openlca.app.wizards.io.EcoSpold01ImportWizard;
import org.openlca.app.wizards.io.ExcelImportWizard;
import org.openlca.app.wizards.io.GeoJsonImportWizard;
import org.openlca.app.wizards.io.ILCDImportWizard;
import org.openlca.app.wizards.io.ImportLibraryDialog;
import org.openlca.app.wizards.io.JsonImportWizard;
import org.openlca.app.wizards.io.SimaProCsvImportWizard;
import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.MappingFileDao;
import org.openlca.core.io.maps.FlowMap;
import org.openlca.io.Format;
import org.openlca.io.openepd.EpdDoc;
import org.openlca.jsonld.Json;

public class FileImport {

	public void run() {
		var path = new FileDialog(UI.shell(), SWT.OPEN).open();
		if (path == null)
			return;
		var file = new File(path);
		if (!file.exists() || file.isDirectory()) {
			MsgBox.error(M.NotAFile, M.NotAnExistingFile + " - " + path);
			return;
		}

		// handle script files
		var name = file.getName().toLowerCase();
		if (name.endsWith(".py")
			|| name.endsWith(".sql")) {
			SaveScriptDialog.forImportOf(file);
			return;
		}

		// check if it is a known import format
		var format = Format.detect(file).orElse(null);
		if (format != null) {
			handleFormat(file, format);
			return;
		}

		// check if it is an openEPD file
		if (name.endsWith(".json")) {
			try {
				var json = Json.read(file).orElse(null);
				if (json != null && json.isJsonObject()) {
					var obj = json.getAsJsonObject();
					if (Objects.equals("OpenEPD", Json.getString(obj, "doctype"))) {
						var epd = EpdDoc.fromJson(obj).orElse(null);
						if (epd != null) {
							ImportDialog.show(epd);
							return;
						}
					}
				}
			} catch (Exception ignored) {
			}
		}

		MsgBox.info(M.UnknownFormat, M.UnknownFormatInfo);

	}

	private void handleFormat(File file, Format format) {
		switch (format) {
			case ES1_XML, ES1_ZIP -> EcoSpold01ImportWizard.of(file);
			case ES2_XML, ES2_ZIP -> MsgBox.info(
					M.EcoSpoldV2, M.EcoSpoldV2Info);
			case EXCEL -> ExcelImportWizard.of(file);
			case GEO_JSON -> GeoJsonImportWizard.of(file);
			case ILCD_ZIP -> ILCDImportWizard.of(file);
			case JSON_LD_ZIP -> JsonImportWizard.of(file);
			case LIBRARY_PACKAGE -> ImportLibraryDialog.open(file);
			case MAPPING_CSV -> importMappingFile(file);
			case SIMAPRO_CSV -> SimaProCsvImportWizard.of(file);
			case ZOLCA -> importZOLCA(file);
			default -> MsgBox.info(M.NoImportFound, M.NoImportFoundInfo);
		}
	}

	private void importMappingFile(File file) {
		var db = Database.get();
		if (db == null) {
			MsgBox.info(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		try {
			var flowMap = FlowMap.fromCsv(file);
			var mapping = flowMap.toMappingFile();

			// guess a new mapping name
			var dao = new MappingFileDao(db);
			var existing = dao.getNames()
				.stream()
				.map(String::toLowerCase)
				.collect(Collectors.toSet());
			var proposed = file.getName();
			var i = 1;
			while (existing.contains(proposed.toLowerCase())) {
				i++;
				proposed = file.getName() + i + ".csv";
			}

			// open a friendly dialog
			var dialog = new InputDialog(
				UI.shell(),
				M.SaveMappingInDatabase,
				M.SaveMappingInDatabaseInfo,
				proposed,
				name -> {
					if (Strings.isBlank(name))
						return M.NameCannotBeEmpty;
					if (existing.contains(name.toLowerCase().trim()))
						return M.FlowMappingWithThisNameExists;
					return null;
				});
			if (dialog.open() != Window.OK)
				return;

			// save it
			mapping.name = dialog.getValue().trim();
			dao.insert(mapping);
			Navigator.refresh();

		} catch (Exception e) {
			ErrorReporter.on("Failed to save as " +
				"mapping file: " + file, e);
		}
	}

	private void importZOLCA(File file) {
		var db = Database.get();
		if (db == null) {
			var b = Question.ask(M.ImportDatabaseQ,
				M.ImportFileAsDatabase + "\r\n" + file.getName());
			if (b) {
				DbRestoreAction.run(file);
			}
			return;
		}
		new ZolcaImportDialog(file, db).open();
	}

	private static class ZolcaImportDialog extends FormDialog {

		private final File zolca;
		private final IDatabase activeDB;
		private boolean intoActiveDB = false;

		ZolcaImportDialog(File zolca, IDatabase activeDB) {
			super(UI.shell());
			this.zolca = zolca;
			this.activeDB = activeDB;
		}

		@Override
		protected void configureShell(Shell shell) {
			super.configureShell(shell);
			shell.setText(M.ImportDatabase);
			shell.setImage(Icon.IMPORT.get());
		}

		@Override
		protected Point getInitialSize() {
			return new Point(600, 400);
		}

		@Override
		protected void createFormContent(IManagedForm form) {
			var tk = form.getToolkit();
			var body = UI.dialogBody(form.getForm(), tk);
			tk.createLabel(body, M.ImportFile + " - " + zolca.getName());

			var opt1 = tk.createButton(body, M.AsStandaloneDatabase, SWT.RADIO);
			opt1.setSelection(!intoActiveDB);
			Controls.onSelect(opt1,
				_e -> intoActiveDB = !opt1.getSelection());

			var opt2 = tk.createButton(body,
					NLS.bind(M.IntoActiveDb, activeDB.getName()),
					SWT.RADIO);
			opt2.setSelection(intoActiveDB);
			Controls.onSelect(opt2,
				_e -> intoActiveDB = opt2.getSelection());

			form.reflow(true);
		}

		@Override
		protected void okPressed() {
			if (intoActiveDB) {
				DbImportWizard.of(zolca);
			} else {
				DbRestoreAction.run(zolca);
			}
			super.okPressed();
		}
	}
}
