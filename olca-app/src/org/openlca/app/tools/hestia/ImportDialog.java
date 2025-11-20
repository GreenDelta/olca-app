package org.openlca.app.tools.hestia;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.AppContext;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.wizards.io.ImportLogDialog;
import org.openlca.core.io.maps.FlowMap;
import org.openlca.core.model.ModelType;
import org.openlca.io.hestia.HestiaClient;
import org.openlca.io.hestia.HestiaImport;
import org.openlca.io.hestia.SearchResult;

public class ImportDialog extends FormDialog {

	private final HestiaClient client;
	private final List<SearchResult> cycles;
	private SettingsPanel settingsPanel;
	private Button unitProcessRadio;
	private Button lciResultRadio;
	private Button linkProvidersCheck;

	public static boolean show(HestiaClient client, List<SearchResult> cycles) {
		if (client == null || cycles == null || cycles.isEmpty())
			return false;
		var dialog = new ImportDialog(client, cycles);
		return dialog.open() == IDialogConstants.OK_ID;
	}

	private ImportDialog(HestiaClient client, List<SearchResult> cycles) {
		super(UI.shell());
		this.client = client;
		this.cycles = cycles;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Import Cycles from Hestia");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.dialogBody(mForm.getForm(), tk);

		createCyclesSection(body, tk);
		createSettingsSection(body, tk);
		createImportOptionsSection(body, tk);

		mForm.reflow(true);
	}

	private void createCyclesSection(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, "Cycles to import (" + cycles.size() + ")");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);

		var table = Tables.createViewer(comp, "Name", "ID");
		UI.gridData(table.getControl(), true, true);
		Tables.bindColumnWidths(table, 0.7, 0.3);
		table.setLabelProvider(new CycleTableLabel());
		table.setInput(cycles);
	}

	private void createSettingsSection(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, "Import Settings");
		var comp = UI.sectionClient(section, tk, 1);

		var db = Database.get();
		if (db != null) {
			settingsPanel = new SettingsPanel(comp, tk);
		} else {
			UI.label(comp, tk, "No database opened - mapping options not available");
		}
	}

	private void createImportOptionsSection(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, "Import Options");
		var comp = UI.sectionClient(section, tk, 1);

		// Import as options
		var typeGroup = UI.composite(comp, tk);
		UI.gridLayout(typeGroup, 1, 10, 0);
		UI.label(typeGroup, tk, "Import as:");

		unitProcessRadio = tk.createButton(typeGroup, "Unit process", SWT.RADIO);
		unitProcessRadio.setSelection(true);

		lciResultRadio = tk.createButton(typeGroup, "LCI result", SWT.RADIO);

		// Link providers option
		linkProvidersCheck = tk.createButton(comp, "Try to link providers", SWT.CHECK);
		linkProvidersCheck.setSelection(true);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Import", true);
		createButton(parent, IDialogConstants.CANCEL_ID, M.Cancel, false);
	}

	@Override
	protected void okPressed() {
		var db = Database.get();
		if (db == null) {
			MsgBox.info(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}

		// Configure import settings
		var flowMap = settingsPanel != null ? settingsPanel.flowMap() : FlowMap.empty();
		var imp = new HestiaImport(client, db, flowMap);
		var log = imp.log();

		// Configure import options based on dialog selections
		// Note: These methods may not exist yet in HestiaImport, but this shows the intended API
		var importAsUnitProcess = unitProcessRadio.getSelection();
		var tryLinkProviders = linkProvidersCheck.getSelection();

		var success = new AtomicReference<Boolean>(false);

		App.runWithProgress(
			"Importing " + cycles.size() + " cycle(s)...",
			() -> {
				try {
					for (var cycle : cycles) {
						var res = imp.importCycle(cycle.id());
						if (res.isError()) {
							log.error("Failed to import " + cycle.name() + ": " + res.error());
						} else {
							log.imported(res.value());
						}
					}
					success.set(true);
				} catch (Exception e) {
					log.error("Import failed: " + e.getMessage());
					success.set(false);
				}
			},
			() -> {
				if (success.get()) {
					Navigator.refresh();
					AppContext.evictAll();
				}
				ImportLogDialog.show("Import finished", log);
				super.okPressed();
			});
	}

	private static class CycleTableLabel extends LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return col == 0 ? Images.get(ModelType.PROCESS) : null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof SearchResult r))
				return null;
			return switch (col) {
				case 0 -> r.name();
				case 1 -> r.id();
				default -> null;
			};
		}
	}
}
