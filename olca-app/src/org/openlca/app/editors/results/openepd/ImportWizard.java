package org.openlca.app.editors.results.openepd;

import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
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
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.util.Strings;

public class ImportWizard extends Wizard implements IImportWizard {

	private final Credentials credentials = Credentials.init();
	private Ec3Client client;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import an EPD result");
	}

	@Override
	public boolean performFinish() {
		credentials.save();
		if (client != null) {
			client.logout();
		}
		return true;
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
			var searchText = new Text(searchComp, SWT.SEARCH);
			UI.gridData(searchText, true, false);
			var button = new Button(searchComp, SWT.PUSH);
			button.setText("Search");

			Controls.onSelect(button, $ -> doSearch());
			Controls.onReturn(searchText, $ -> doSearch());

			table = Tables.createViewer(
				root, "EPD", "Manufacturer", "Published", "Valid until");
			UI.gridData(table.getControl(), true, true);
			table.setLabelProvider(new TableLabel());
		}

		private void credentialFields(Composite comp) {
			var urlText = UI.formText(comp, "URL");
			urlText.setText(Strings.orEmpty(credentials.url));
			urlText.addModifyListener(
				$ -> credentials.url = urlText.getText());

			var userText = UI.formText(comp, "User");
			userText.setText(Strings.orEmpty(credentials.user));
			userText.addModifyListener(
				$ -> credentials.user = userText.getText());

			var pwText = UI.formText(comp, "Password", SWT.PASSWORD);
			pwText.setText(Strings.orEmpty(credentials.password));
			pwText.addModifyListener(
				$ -> credentials.password = pwText.getText());
		}

		private void doSearch() {
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

			var descriptors = new ArrayList<EpdDescriptor>();
			App.runWithProgress("Fetch epds", () -> {
				try {
					var epds = client.get("epds", JsonArray.class);
					for (var elem : epds) {
						if (elem == null || !elem.isJsonObject())
							continue;
						var d = EpdDescriptor.from(elem.getAsJsonObject());
						descriptors.add(d);
					}
				} catch (Exception e) {
					ErrorReporter.on("Failed to search for EPDs", e);
				}
			}, () -> table.setInput(descriptors));
		}

		private class TableLabel extends BaseLabelProvider
			implements ITableLabelProvider {

			@Override
			public Image getColumnImage(Object obj, int col) {
				return null;
			}

			@Override
			public String getColumnText(Object obj, int col) {
				if (!(obj instanceof EpdDescriptor))
				return null;
				var epd = (EpdDescriptor) obj;
				return switch (col) {
					case 0 -> epd.name();
					case 2 -> epd.publicationDate();
					default -> null;
				};
			}
		}

	}
}
