package org.openlca.app.tools.libraries;

import java.io.File;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationCombo;
import org.openlca.core.database.IDatabase;
import org.openlca.core.library.LibraryExport;
import org.openlca.core.library.LibraryInfo;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.Version;
import org.openlca.util.Databases;

public class LibraryExportDialog extends FormDialog {

	private final Config config;

	private LibraryExportDialog(Config config) {
		super(UI.shell());
		this.config = config;
		config.name = config.db.getName();
		config.version = "1.0";
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
		try {
			var config = App.exec(
				"Collect database properties...",
				() -> Config.of(db));
			if (!config.dbHasInventory) {
				MsgBox.error("No inventory data",
					"The library export of databases without inventory data" +
						" is not supported yet.");
				return;
			}
			new LibraryExportDialog(config).open();
		} catch (Exception e) {
			ErrorReporter.on("Failed to open library export dialog", e);
		}
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

		BiFunction<String, Consumer<Boolean>, Button> check =
			(label, onClick) -> {
				UI.filler(body, tk);
				var button = tk.createButton(body, label, SWT.CHECK);
				Controls.onSelect(button,
					_e -> onClick.accept(button.getSelection()));
				return button;
			};

		if (config.dbHasImpacts) {
			check.apply(
					"With LCIA data",
					b -> config.withImpacts = b)
				.setSelection(config.withImpacts);
		}

		if (config.dbHasUncertainties) {
			check.apply(
					"With uncertainty distributions",
					b -> config.withUncertainties = b)
				.setSelection(config.withUncertainties);
		}

		check.apply(
				"Regionalized",
				b -> config.regionalized = b)
			.setSelection(config.regionalized);

		// data quality values
		if (config.dbDQSystem != null) {
			var dqCheck = check.apply(
				"With data quality values (" + config.dbDQSystem.name + ")",
				b -> config.dqSystem = b
					? config.dbDQSystem
					: null);
			dqCheck.setSelection(config.dqSystem != null);
			dqCheck.setEnabled(false); // disabled for now
		}
	}

	@Override
	protected void okPressed() {
		var libDir = Workspace.getLibraryDir();
		var info = config.toInfo();
		var id = info.toId();
		if (libDir.hasLibrary(id)) {
			MsgBox.error("Library " + id + " already exists",
				"A library with this name and version already exists");
			return;
		}
		var exportDir = new File(libDir.folder(), id);
		super.okPressed();
		var export = new LibraryExport(config.db, exportDir)
			.withConfig(info)
			.withImpacts(config.withImpacts)
			.withAllocation(config.allocation)
			.withUncertainties(config.withUncertainties);
		App.runWithProgress(
			"Creating library " + id,
			export,
			Navigator::refresh);
	}

	private static class Config {

		private final IDatabase db;

		// database properties that indicate
		// which configuration options we
		// can provide
		final boolean dbHasInventory;
		final boolean dbHasImpacts;
		final boolean dbHasUncertainties;
		final DQSystem dbDQSystem;

		String name;
		String version;
		AllocationMethod allocation;
		boolean regionalized;
		boolean withImpacts;
		boolean withUncertainties;
		DQSystem dqSystem;

		private Config(IDatabase db) {
			this.db = db;
			this.dbHasInventory = Databases.hasInventoryData(db);
			this.dbHasImpacts = Databases.hasImpactData(db);
			this.dbHasUncertainties = Databases.hasUncertaintyData(db);
			this.dbDQSystem = Databases.getCommonFlowDQS(db).orElse(null);
		}

		static Config of(IDatabase db) {
			return new Config(db);
		}

		LibraryInfo toInfo() {
			return LibraryInfo.of(name)
				.version(version)
				.isRegionalized(regionalized);
		}
	}
}
