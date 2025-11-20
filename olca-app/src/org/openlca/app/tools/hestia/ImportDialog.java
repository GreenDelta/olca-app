package org.openlca.app.tools.hestia;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
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
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.wizards.io.ImportLogDialog;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProcessType;
import org.openlca.io.hestia.HestiaClient;
import org.openlca.io.hestia.HestiaImport;
import org.openlca.io.hestia.SearchResult;

public class ImportDialog extends FormDialog {

	private final IDatabase db;
	private final HestiaClient client;
	private final List<SearchResult> cycles;

	private MappingCombo mappingCombo;
	private ProcessType processType = ProcessType.UNIT_PROCESS;
	private boolean linkProviders = true;

	public static void show(HestiaClient client, List<SearchResult> cycles) {
		if (client == null || cycles == null || cycles.isEmpty())
			return;
		var db = Database.get();
		if (db == null) {
			MsgBox.error(M.NoDatabaseOpened, M.NoDatabaseOpenedImportInfo);
			return;
		}
		var dialog = new ImportDialog(db, client, cycles);
		dialog.open();
	}

	private ImportDialog(
		IDatabase db, HestiaClient client, List<SearchResult> cycles) {
		super(UI.shell());
		this.db = db;
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
		return new Point(800, 400);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.dialogBody(mForm.getForm(), tk);
		createCyclesTable(body, tk);
		createImportSettings(body, tk);
		mForm.reflow(true);
	}

	private void createCyclesTable(Composite comp, FormToolkit tk) {
		var table = Tables.createViewer(comp, "Name", "ID");
		UI.gridData(table.getControl(), true, true);
		Tables.bindColumnWidths(table, 0.7, 0.3);
		table.setLabelProvider(new CycleTableLabel());
		table.setInput(cycles);
	}

	private void createImportSettings(Composite parent, FormToolkit tk) {
		var comp = tk.createComposite(parent);
		UI.gridLayout(comp, 2, 10, 5);
		UI.gridData(comp, true, false);

		var combo = UI.labeledCombo(comp, tk, "Flow mapping:");
		mappingCombo = new MappingCombo(db, combo);

		UI.label(comp, tk, "Import as:");
		var radioComp = tk.createComposite(comp);
		UI.gridLayout(radioComp, 2, 10, 0);

		var uRadio = tk.createButton(radioComp, M.UnitProcess, SWT.RADIO);
		uRadio.setSelection(processType == ProcessType.UNIT_PROCESS);

		var sRadio = tk.createButton(radioComp, M.SystemProcess, SWT.RADIO);
		sRadio.setSelection(processType == ProcessType.LCI_RESULT);

		UI.filler(comp, tk);
		var providerCheck = tk.createButton(
			comp, "Try to link providers", SWT.CHECK);
		providerCheck.setSelection(linkProviders);

		Consumer<SelectionEvent> onSelect = e -> {
			if (e.widget == uRadio) {
				processType = uRadio.getSelection()
					? ProcessType.UNIT_PROCESS
					: ProcessType.LCI_RESULT;
				providerCheck.setEnabled(uRadio.getSelection());

			} else if (e.widget == sRadio) {
				processType = sRadio.getSelection()
					? ProcessType.LCI_RESULT
					: ProcessType.UNIT_PROCESS;
				providerCheck.setEnabled(!sRadio.getSelection());
			}
		};

		Controls.onSelect(uRadio, onSelect);
		Controls.onSelect(sRadio, onSelect);
		Controls.onSelect(providerCheck, e -> {
			linkProviders = providerCheck.getSelection();
		});
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

		var flowMap = mappingCombo.getFlowMap();
		var imp = new HestiaImport(client, db, flowMap)
			.withProcessType(processType)
			.withProviderLinks(linkProviders);
		var log = imp.log();

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
				} catch (Exception e) {
					log.error("Import failed: " + e.getMessage());
				}
			},
			() -> {
				Navigator.refresh();
				AppContext.evictAll();
				ImportLogDialog.show(M.ImportFinished, log);
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
