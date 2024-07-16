package org.openlca.app.tools.soda;

import java.util.List;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
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
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.FileType;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.wizards.io.ImportLogDialog;
import org.openlca.ilcd.commons.LangString;
import org.openlca.ilcd.descriptors.Descriptor;
import org.openlca.ilcd.io.SodaClient;
import org.openlca.io.ilcd.input.Import;
import org.openlca.util.Strings;

class SodaPage extends FormPage {

	private final Connection con;
	private final SodaClient client;
	private TableViewer table;

	SodaPage(SodaClientTool tool, Connection con) {
		super(tool, "SodaClientTool.Page", "soda4LCA");
		this.con = con;
		this.client = con.client();
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm, "soda4LCA");
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
		var comp = UI.formSection(body, tk, M.Connection);
		var urlText = UI.labeledText(comp, tk, "URL");
		urlText.setText(con.toString());
		urlText.setEditable(false);
		createStockCombo(comp, tk);
	}

	private void createStockCombo(Composite outer, FormToolkit tk) {
		if (con.stocks().isEmpty())
			return;

		UI.label(outer, tk, M.DataStock);
		var inner = UI.composite(outer, tk);
		UI.fillHorizontal(inner);
		var grid = UI.gridLayout(inner, 2);
		grid.marginWidth = 0;
		grid.marginTop = 0;

		var combo = new Combo(inner, SWT.READ_ONLY);
		UI.fillHorizontal(combo);
		var items = new String[con.stocks().size() + 1];
		items[0] = M.UndefinedDefault;
		int i = 1;
		for (var stock : con.stocks()) {
			items[i] = stock.getShortName();
			i++;
		}

		combo.setItems(items);
		combo.select(0);

		var downloadBtn = UI.button(inner, tk, M.Download);
		downloadBtn.setImage(Images.get(FileType.ZIP));
		downloadBtn.setEnabled(false);

		Controls.onSelect(combo, $ -> {
			int k = combo.getSelectionIndex();
			if (k == 0) {
				client.useDataStock(null);
				downloadBtn.setEnabled(false);
				return;
			}
			var stock = con.stocks().get(k - 1);
			var stockId = stock != null
					? stock.getUUID()
					: null;
			client.useDataStock(stockId);
			downloadBtn.setEnabled(stockId != null);
		});

		Controls.onSelect(downloadBtn, $ -> {
			int k = combo.getSelectionIndex();
			if (k == 0)
				return;
			var stock = con.stocks().get(k - 1);
			StockDownload.run(client, stock);
		});
	}

	private void createDataSection(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, M.DataSet);
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);
		var search = SearchBar.create(con, comp, tk);

		table = Tables.createViewer(comp, M.Name, "UUID", M.Version, M.Comment);
		UI.gridData(table.getControl(), true, true);
		Tables.bindColumnWidths(table, 0.3, 0.2, 0.2, 0.3);
		table.setLabelProvider(new TableLabel(con.hasEpds()));
		var importAction = Actions.create(
				M.ImportSelected, Icon.IMPORT.descriptor(), this::runImport);
		var copyAction = TableClipboard.onCopySelected(table);
		Actions.bind(table, importAction, copyAction);
		search.onResults(table::setInput);
	}

	private void runImport() {
		List<Descriptor<?>> selection = Viewers.getAllSelected(table);
		if (selection.isEmpty())
			return;

		var db = Database.get();
		if (db == null) {
			MsgBox.info(M.NoDatabaseOpened, M.NoDatabaseOpenedImportInfo);
			return;
		}

		var b = Question.ask(M.ImportSelectedDataSetQ,
				M.ImportSelectedDataSetQuestion);
		if (!b)
			return;

		var imp = Import.of(client, db)
				.withPreferredLanguage(IoPreference.getIlcdLanguage());
		App.runWithProgress(M.ImportDataSetsDots, () -> {
			for (var d : selection) {
				imp.write(d.toRef().getType(), d.getUUID());
			}
		}, () -> {
			ImportLogDialog.show(imp);
			Navigator.refresh();
		});
	}

	private static class TableLabel extends BaseLabelProvider
			implements ITableLabelProvider {

		private final boolean hasEpds;
		private final String lang = IoPreference.getIlcdLanguage();

		TableLabel(boolean hasEpds) {
			this.hasEpds = hasEpds;
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			return col == 0 && obj instanceof Descriptor<?> d
					? Util.imageOf(d.toRef().getType(), hasEpds)
					: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Descriptor<?> d))
				return null;
			return switch (col) {
				case 0 -> LangString.getOrDefault(d.getName(), lang);
				case 1 -> d.getUUID();
				case 2 -> d.getVersion();
				case 3 ->
						Strings.cut(LangString.getOrDefault(d.getComment(), lang), 75);
				default -> null;
			};
		}

	}
}
