package org.openlca.app.tools.openepd.input;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.App;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.openepd.model.Api;
import org.openlca.app.tools.openepd.model.Credentials;
import org.openlca.app.tools.openepd.model.Ec3CategoryTree;
import org.openlca.app.tools.openepd.model.Ec3Client;
import org.openlca.app.tools.openepd.model.Ec3Epd;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Selections;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.jsonld.Json;
import org.openlca.util.Strings;

@Deprecated
public class DownloadWizard extends Wizard implements IImportWizard {

	private final Credentials credentials = Credentials.getDefault();
	private Ec3Client client;
	private Ec3Epd epd;
	private Ec3CategoryTree categories;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import an EPD result");
		setDefaultPageImageDescriptor(Icon.EC3_WIZARD.descriptor());
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		credentials.save();
		if (client != null) {
			client.logout();
		}
		var db = Database.get();
		if (epd == null || db == null)
			return false;
		try {
			var ok = new AtomicBoolean(false);
			getContainer().run(false, false, monitor -> {
				var fullEpd = Api.getEpd(client, epd.id);
				if (fullEpd.isEmpty()) {
					ErrorReporter.on("Failed to download EPD " + epd.id);
				} else {
					int r = ImportDialog.show(fullEpd.get(), categories);
					ok.set(r == Window.OK);
				}
			});
			return ok.get();
		} catch (Exception e) {
			ErrorReporter.on("Failed to download EPD " + epd.id, e);
			return false;
		}
	}

	@Override
	public boolean performCancel() {
		if (client != null) {
			client.logout();
		}
		return true;
	}

	@Override
	public void addPages() {
		addPage(new Page());
	}

	private class Page extends WizardPage {

		private TableViewer table;
		private Text queryText;

		Page() {
			super("Search");
			setTitle("Search for EPD results");
			setDescription("Login with your EC3 account and search for an EPD");
			setPageComplete(false);
		}

		@Override
		public void createControl(Composite parent) {
			var root = new Composite(parent, SWT.NONE);
			setControl(root);
			UI.gridLayout(root, 1, 0, 5);
			root.setLayout(new GridLayout(1, false));

			var comp = new Composite(root, SWT.NONE);
			UI.gridData(comp, true, false);
			UI.gridLayout(comp, 2);
			credentialFields(comp);

			// search field
			UI.formLabel(comp, "EPD");
			var searchComp = new Composite(comp, SWT.NONE);
			UI.gridData(searchComp, true, false);
			UI.gridLayout(searchComp, 2, 10, 0);
			queryText = new Text(searchComp, SWT.SEARCH);
			UI.gridData(queryText, true, false);
			var button = new Button(searchComp, SWT.PUSH);
			button.setText("Search");
			Controls.onSelect(button, $ -> onSearch());
			Controls.onReturn(queryText, $ -> onSearch());

			// descriptor table
			table = Tables.createViewer(
				root, "EPD", "Manufacturer", "Category", "Declared unit");
			Tables.bindColumnWidths(table, 0.25, 0.25, 0.25, 0.25);
			UI.gridData(table.getControl(), true, true);
			table.setLabelProvider(new TableLabel());
			table.addSelectionChangedListener(e -> {
				Object first = Selections.firstOf(e.getSelection());
				epd = first instanceof Ec3Epd
					? (Ec3Epd) first
					: null;
				setPageComplete(epd != null);
			});

			// save-as-file action
			var onSaveFile = Actions.create(
				"Save as file", Icon.FILE.descriptor(), () -> {
					Ec3Epd epd = Viewers.getFirstSelected(table);
					if (epd == null)
						return;
					var file = FileChooser.forSavingFile(
						"Save openEPD", epd.name + ".json");
					if (file == null)
						return;
					var json = App.exec(
						"Download EPD", () -> Api.getRawEpd(client, epd.id));
					if (json.isEmpty()) {
						MsgBox.error("Failed to download EPD " + epd.id);
						return;
					}
					Json.write(json.get(), file);
				});
			Actions.bind(table, onSaveFile);
		}

		private void credentialFields(Composite comp) {
			var urlText = UI.formText(comp, "URL");
			urlText.setText(Strings.orEmpty(credentials.url()));
			urlText.addModifyListener(
				$ -> credentials.url(urlText.getText()));

			var userText = UI.formText(comp, "User");
			userText.setText(Strings.orEmpty(credentials.user()));
			userText.addModifyListener(
				$ -> credentials.user(userText.getText()));

			var pwText = UI.formText(comp, "Password", SWT.PASSWORD);
			pwText.setText(Strings.orEmpty(credentials.password()));
			pwText.addModifyListener(
				$ -> credentials.password(pwText.getText()));
		}

		private void onSearch() {
			epd = null;
			setPageComplete(false);

			// login into EC3
			if (client == null) {
				var c = credentials.login();
				if (c.isEmpty()) {
					MsgBox.error("Login failed",
						"Failed to login into the EC3 API with the given" +
							" user name and password. Check the log-file " +
							"for further details.");
					return;
				}
				client = c.get();
			}

			// load the category index once
			if (categories == null) {
				categories = App.exec(
					"Fetch categories", () -> Api.getCategoryTree(client));
			}

			var query = queryText.getText().trim();

			// if the query field contains an http* URL, we try
			// to directly fetch the EPD
			if (query.toLowerCase().startsWith("http")) {
				var parts = query.split("/");
				if (parts.length > 2) {
					var epd = App.exec("Fetch EPD", () -> {
						var id = parts[parts.length - 1];
						return Api.getEpd(client, id);
					});
					if (epd.isPresent()) {
						ImportDialog.show(epd.get(), categories);
						DownloadWizard.this.performCancel();
						DownloadWizard.this.getShell().close();
						return;
					}
				}
			}

			// otherwise, search for EPDs
			var epds = new ArrayList<Ec3Epd>();
			App.runWithProgress("Fetch EPDs", () -> {
				var response = Api.descriptors(client).query(query).get();
				epds.addAll(response.descriptors());
			}, () -> table.setInput(epds));
		}

		private class TableLabel extends BaseLabelProvider
			implements ITableLabelProvider {

			@Override
			public Image getColumnImage(Object obj, int col) {
				return col == 0 ? Icon.BUILDING.get() : null;
			}

			@Override
			public String getColumnText(Object obj, int col) {
				if (!(obj instanceof Ec3Epd epd))
					return null;
				return switch (col) {
					case 0 -> epd.name;
					case 1 -> epd.manufacturer != null
						? epd.manufacturer.name
						: null;
					case 2 -> epd.category != null && categories != null
						? categories.pathOf(epd.category.id)
						: null;
					case 3 -> epd.declaredUnit;
					default -> null;
				};
			}
		}

	}
}
