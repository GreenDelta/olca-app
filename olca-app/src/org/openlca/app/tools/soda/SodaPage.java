package org.openlca.app.tools.soda;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.preferences.IoPreference;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.ilcd.commons.DataSetType;
import org.openlca.ilcd.commons.LangString;
import org.openlca.ilcd.descriptors.Descriptor;
import org.openlca.ilcd.io.SodaClient;
import org.openlca.io.ilcd.input.Import;
import org.openlca.util.Strings;

import java.util.ArrayList;
import java.util.List;

class SodaPage extends FormPage {

	private final Connection con;
	private final SodaClient client;
	private TableViewer table;

	SodaPage(SodaClientTool tool, Connection con) {
		super(tool, "SodaClientTool.Page", "soda4LCA client");
		this.con = con;
		this.client = con.client();
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm, "soda4LCA client");
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		createConnectionSection(body, tk);
		createDataSection(body, tk);
		body.addDisposeListener($ -> {
			if (client != null) {
				client.close();
			}
		});
	}

	private void createConnectionSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Connection");
		var urlText = UI.labeledText(comp, tk, "URL");
		urlText.setText(con.toString());
		urlText.setEditable(false);
		createStockCombo(comp, tk);
	}

	private void createStockCombo(Composite comp, FormToolkit tk) {
		if (con.stocks().isEmpty())
			return;
		var combo = UI.labeledCombo(comp, tk, "Data stock");
		var items = new String[con.stocks().size() + 1];
		items[0] = "Undefined / Default";
		int i = 1;
		for (var stock : con.stocks()) {
			items[i] = stock.getShortName();
			i++;
		}

		combo.setItems(items);
		combo.select(0);
		Controls.onSelect(combo, $ -> {
			int k = combo.getSelectionIndex();
			if (k == 0) {
				client.useDataStock(null);
				return;
			}
			var stock = con.stocks().get(k - 1);
			client.useDataStock(stock != null
					? stock.getUUID()
					: null);
		});

	}

	private void createDataSection(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "Datasets");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);

		var searchComp = tk.createComposite(comp);
		UI.fillHorizontal(searchComp);
		UI.gridLayout(searchComp, 4);

		var typeCombo = TypeCombo.create(searchComp, tk);
		var searchText = tk.createText(searchComp, "", SWT.BORDER);
		UI.fillHorizontal(searchText);
		searchText.setMessage("Search dataset ...");
		var button = tk.createButton(searchComp, "Search", SWT.NONE);
		Controls.onSelect(button,
				e -> runSearch(typeCombo.selected(), searchText.getText()));
		searchText.addTraverseListener(e -> {
			if (e.detail == SWT.TRAVERSE_RETURN) {
				runSearch(typeCombo.selected(), searchText.getText());
			}
		});

		table = Tables.createViewer(comp, M.Name, "UUID", M.Version, M.Comment);
		UI.gridData(table.getControl(), true, true);
		Tables.bindColumnWidths(table, 0.3, 0.2, 0.2, 0.3);
		table.setLabelProvider(new TableLabel());
		var importAction = Actions.create(
				"Import selected", Icon.IMPORT.descriptor(), this::runImport);
		Actions.bind(table, importAction);
	}

	private void runSearch(DataSetType type, String name) {
		var clazz = Util.classOf(type);
		if (clazz == null)
			return;

		var err = new String[1];
		var result = new ArrayList<Descriptor<?>>();
		App.run("Search datasets ...", () -> {
			try {
				var list = client.search(clazz, name);
				result.addAll(list.getDescriptors());
			} catch (Exception e) {
				err[0] = e.getMessage();
			}
		}, () -> {
			if (err[0] == null)
				table.setInput(result);
			else
				MsgBox.error("Searching for datasets failed", err[0]);
		});
	}

	private void runImport() {
		List<Descriptor<?>> selection = Viewers.getAllSelected(table);
		if (selection.isEmpty())
			return;

		var db = Database.get();
		if (db == null) {
			MsgBox.info("No database open",
					"You need to open the database where" +
							" you want to import the datasets first.");
			return;
		}

		App.runWithProgress("Import datasets ...", () -> {
			var imp = Import.of(client, db)
					.withLanguageOrder(IoPreference.getIlcdLanguage());
			for (var d : selection) {
				imp.write(d.toRef().getType(), d.getUUID());
			}
		}, Navigator::refresh);
	}

	private static class TableLabel extends BaseLabelProvider
			implements ITableLabelProvider {

		private final String lang = IoPreference.getIlcdLanguage();

		@Override
		public Image getColumnImage(Object obj, int col) {
			return col == 0 && obj instanceof Descriptor<?> d
					? Util.imageOf(d.toRef().getType())
					: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Descriptor<?> d))
				return null;
			return switch (col) {
				case 0 -> LangString.getFirst(d.getName(), lang);
				case 1 -> d.getUUID();
				case 2 -> d.getVersion();
				case 3 -> Strings.cut(LangString.getFirst(d.getComment(), lang), 75);
				default -> null;
			};
		}

	}
}
