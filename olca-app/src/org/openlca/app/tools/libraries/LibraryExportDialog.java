package org.openlca.app.tools.libraries;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationCombo;
import org.openlca.core.database.IDatabase;
import org.openlca.core.library.LibraryExport;
import org.openlca.core.library.LibraryInfo;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Version;

public class LibraryExportDialog extends FormDialog {

	private final IDatabase db;
	private final Config config;

	private LibraryExportDialog(IDatabase db) {
		super(UI.shell());
		this.db = db;
		this.config = new Config();
		config.name = db.getName();
		config.version = "0.0.1";
		config.allocation = AllocationMethod.NONE;
	}

	public static void show() {
		var db = Database.get();
		if (db == null) {
			MsgBox.error(M.NoDatabaseOpened,
					"You need to open the database first from which" +
							" you want to create the library.");
			return;
		}
		new LibraryExportDialog(db).open();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Create a library");
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var tk = mform.getToolkit();
		var body = UI.formBody(mform.getForm(), tk);
		UI.gridLayout(body, 2);

		var name = UI.formText(body, tk, M.Name);
		name.setText(config.name);
		name.addModifyListener(_e -> 
			config.name = name.getText().trim());

		var version = UI.formText(body, tk, M.Version);
		version.setText(config.version);
		version.addModifyListener(_e -> 
			config.version = Version.format(version.getText()));

		UI.formLabel(body, tk, M.AllocationMethod);
		var allocCombo = new AllocationCombo(
				body, AllocationMethod.values());
		allocCombo.select(config.allocation);
		allocCombo.addSelectionChangedListener(
				m -> config.allocation = m);

		var regioCheck = UI.formCheckBox(body, tk, "Regionalized");
		regioCheck.setSelection(config.regionalized);
		Controls.onSelect(regioCheck, _e -> 
			config.regionalized = regioCheck.getSelection());
	}

	@Override
	protected void okPressed() {
		var libDir = Workspace.getLibraryDir();
		var info = config.toInfo();
		var exportDir = libDir.getFolder(info);
		var name = info.name + " " + info.version;
		if (libDir.exists(info)) {
			MsgBox.error(name + " already exists",
					"A library with this name and version already exists");
			return;
		}
		super.okPressed();
		var export = new LibraryExport(db, exportDir)
				.as(info)
				.solver(App.getSolver());
		App.runWithProgress(
				"Creating library " + name,
				export,
				Navigator::refresh);
	}

	private static class Config {
		String name;
		String version;
		AllocationMethod allocation;
		boolean regionalized;

		LibraryInfo toInfo() {
			var info = new LibraryInfo();
			info.name = name;
			info.version = version;
			info.isRegionalized = regionalized;
			return info;
		}
	}
}
