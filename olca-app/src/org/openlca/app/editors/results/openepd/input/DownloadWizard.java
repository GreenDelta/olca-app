package org.openlca.app.editors.results.openepd.input;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

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
import org.openlca.app.db.Database;
import org.openlca.app.editors.results.openepd.model.Credentials;
import org.openlca.app.editors.results.openepd.model.Ec3Category;
import org.openlca.app.editors.results.openepd.model.Ec3CategoryIndex;
import org.openlca.app.editors.results.openepd.model.Ec3Client;
import org.openlca.app.editors.results.openepd.model.Ec3Epd;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Selections;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.util.Strings;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class DownloadWizard extends Wizard implements IImportWizard {

	private final Credentials credentials = Credentials.getDefault();
	private Ec3Client client;
	private Ec3Epd epd;
	private Ec3CategoryIndex categories;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import an EPD result");
		setDefaultPageImageDescriptor(Icon.EC3_WIZARD.descriptor());
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
		var status = ImportDialog.show(epd, categories);
		return status == Window.OK;
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

			table = Tables.createViewer(
					root, "EPD", "Category", "Declared unit");
			Tables.bindColumnWidths(table, 0.4, 0.4, 0.2);
			UI.gridData(table.getControl(), true, true);
			table.setLabelProvider(new TableLabel());

			table.addSelectionChangedListener(e -> {
				Object first = Selections.firstOf(e.getSelection());
				epd = first instanceof Ec3Epd
						? (Ec3Epd) first
						: null;
				setPageComplete(epd != null);
			});
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

			var query = createQuery();
			var epds = new ArrayList<Ec3Epd>();
			App.runWithProgress("Fetch EPDs", () -> {
				try {

					// load the category index once
					if (categories == null) {
						var root = client.get("categories/root", JsonObject.class);
						if (root != null) {
							Ec3Category.fromJson(root).ifPresent(
									c -> categories = Ec3CategoryIndex.of(c));
						}
					}

					var array = client.get(query, JsonArray.class);
					for (var elem : array) {
						Ec3Epd.fromJson(elem).ifPresent(epds::add);
					}
				} catch (Exception e) {
					ErrorReporter.on("Failed to search for EPDs", e);
				}
			}, () -> table.setInput(epds));
		}

		private String createQuery() {
			var q = queryText.getText().trim();
			var prefix = "epds?page_size=10";
			return Strings.nullOrEmpty(q)
					? prefix
					: prefix + "&q=" + URLEncoder.encode(q, StandardCharsets.UTF_8);
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
				case 1 -> epd.category != null && categories != null
						? categories.pathOf(epd.category.id)
						: null;
				case 2 -> epd.declaredUnit;
				default -> null;
				};
			}
		}

	}
}
