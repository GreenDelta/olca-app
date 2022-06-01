package org.openlca.app.tools.libraries;

import java.io.File;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
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
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.Process;
import org.openlca.util.Databases;
import org.openlca.util.Strings;

public class LibraryExportDialog extends FormDialog {

	private final Props props;
	private final Config config;

	private LibraryExportDialog(Props props) {
		super(UI.shell());
		this.props = props;
		this.config = new Config();
		config.name = props.db.getName();
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
			var props = App.exec(
				"Collect database properties...",
				() -> Props.of(db));
			if (props.hasLibraryProcesses) {
				MsgBox.error("Contains library processes",
					"The database is already connected to a process library. Libraries" +
						" with dependencies to process libraries are not supported.");
				return;
			}
			new LibraryExportDialog(props).open();
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
	protected Point getInitialSize() {
		int height = 350;
		boolean[] adds = {
			props.hasInventory,
			props.hasUncertainty,
			props.hasInventory || props.hasImpacts,
			props.hasInventory && props.flowDqs != null
		};
		for (var ignored : adds) {
			height += 25;
		}
		UI.initialSizeOf(this, 600, height);
		return super.getInitialSize();
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

		// allocation method
		if (props.hasInventory) {
			UI.formLabel(body, tk, M.AllocationMethod);
			var allocCombo = new AllocationCombo(
				body, AllocationMethod.values());
			allocCombo.select(config.allocation);
			allocCombo.addSelectionChangedListener(
				m -> config.allocation = m);
		}

		BiFunction<String, Consumer<Boolean>, Button> check =
			(label, onClick) -> {
				UI.filler(body, tk);
				var button = tk.createButton(body, label, SWT.CHECK);
				Controls.onSelect(button,
					_e -> onClick.accept(button.getSelection()));
				return button;
			};

		// uncertainty check
		if (props.hasUncertainty) {
			check.apply(
					"With uncertainty distributions",
					b -> config.withUncertainties = b)
				.setSelection(config.withUncertainties);
		}

		// regionalization check
		if (props.hasInventory || props.hasImpacts) {
			check.apply(
					"Regionalized",
					b -> config.regionalized = b)
				.setSelection(config.regionalized);
		}

		// data quality check
		if (props.hasInventory && props.flowDqs != null) {
			var dqCheck = check.apply(
				"With data quality values (" + props.flowDqs.name + ")",
				b -> config.dqSystem = b
					? props.flowDqs
					: null);
			dqCheck.setSelection(config.dqSystem != null);
			dqCheck.setEnabled(false); // disabled for now
		}
	}

	@Override
	protected void okPressed() {
		var libDir = Workspace.getLibraryDir();
		var info = config.toInfo();
		var id = info.name();
		if (libDir.hasLibrary(id)) {
			MsgBox.error("Library " + id + " already exists",
				"A library with this name and version already exists");
			return;
		}
		var exportDir = new File(libDir.folder(), id);
		super.okPressed();
		var export = new LibraryExport(props.db, exportDir)
			.withConfig(info)
			.withAllocation(config.allocation)
			.withUncertainties(config.withUncertainties);
		App.runWithProgress(
			"Creating library " + id,
			export,
			Navigator::refresh);
	}

	private static class Config {
		String name;
		AllocationMethod allocation;
		boolean regionalized;
		boolean withUncertainties;
		DQSystem dqSystem;

		LibraryInfo toInfo() {
			return LibraryInfo.of(name)
				.isRegionalized(regionalized);
		}
	}

	private record Props(
		IDatabase db,
		boolean hasLibraryProcesses,
		boolean hasInventory,
		boolean hasImpacts,
		boolean hasUncertainty,
		DQSystem flowDqs
	) {
		static Props of(IDatabase db) {
			boolean hasLibraryProcesses = false;
			boolean hasInventory = false;
			for (var d : db.getDescriptors(Process.class)) {
				hasInventory = true;
				if (Strings.notEmpty(d.library)) {
					hasLibraryProcesses = true;
					break;
				}
			}
			boolean hasImpacts = false;
			for (var d : db.getDescriptors(ImpactCategory.class)) {
				if (Strings.nullOrEmpty(d.library)) {
					hasImpacts = true;
					break;
				}
			}

			if (hasLibraryProcesses)
				return new Props(db, true, true, hasImpacts, false, null);

			return new Props(
				db,
				false,
				hasInventory,
				hasImpacts,
				Databases.hasUncertaintyData(db),
				Databases.getCommonFlowDQS(db).orElse(null));
		}
	}
}
