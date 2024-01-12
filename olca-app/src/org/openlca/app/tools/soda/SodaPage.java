package org.openlca.app.tools.soda;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.ilcd.commons.DataSetType;
import org.openlca.ilcd.io.SodaClient;

class SodaPage extends FormPage {

	private final Connection con;
	private final SodaClient client;

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
			items[i] = stock.shortName;
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
					? stock.uuid
					: null);
		});

	}

	private void createDataSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Datasets", 1);

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

		/*
		table = Tables.createViewer(parent, M.Name, M.UUID, M.DataSetVersion,
				M.Comment);
		Tables.bindColumnWidths(table, 0.3, 0.2, 0.2, 0.3);
		table.setLabelProvider(new TableLabel());

		 */
	}

	private void runSearch(DataSetType type, String name) {

	}

}
