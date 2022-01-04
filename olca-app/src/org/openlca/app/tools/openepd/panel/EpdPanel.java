package org.openlca.app.tools.openepd.panel;

import java.util.ArrayList;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.openepd.model.Api;
import org.openlca.app.tools.openepd.model.Ec3CategoryIndex;
import org.openlca.app.tools.openepd.model.Ec3Epd;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;

public class EpdPanel extends SimpleFormEditor {

	public static void open() {
		Editors.open(new SimpleEditorInput("EpdPanel", "openEPD"), "EpdPanel");
	}

	@Override
	protected FormPage getPage() {
		return new Page(this);
	}

	private static class Page extends FormPage {

		private TableViewer table;
		private Ec3CategoryIndex categories;

		public Page(EpdPanel panel) {
			super(panel, "EpdPanel.Page", "openEPD");
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(mform, "Building Transparency - openEPD",
					Icon.EC3_WIZARD.get());
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);
			var loginPanel = LoginPanel.create(body, tk);

			var section = UI.section(body, tk, "Find EPDs");
			UI.gridData(section, true, true);
			var comp = UI.sectionClient(section, tk, 1);
			var searchComp = tk.createComposite(comp);
			UI.fillHorizontal(searchComp);
			UI.gridLayout(searchComp, 4);

			var searchText = tk.createText(searchComp, "");
			UI.fillHorizontal(searchText);
			var searchButton = tk.createButton(searchComp, "Search", SWT.NONE );
			searchButton.setImage(Icon.SEARCH.get());

			tk.createLabel(searchComp, "Max. count:");
			var spinner = new Spinner(searchComp, SWT.BORDER);
			spinner.setValues(100, 10, 1000, 0, 50, 100);
			tk.adapt(spinner);

			// descriptor table
			table = Tables.createViewer(
				comp, "EPD", "Manufacturer", "Category", "Declared unit");
			Tables.bindColumnWidths(table, 0.25, 0.25, 0.25, 0.25);
			UI.gridData(table.getControl(), true, true);
			table.setLabelProvider(new TableLabel());

			// search handler
			Runnable onSearch = () -> {
				var client = loginPanel.login().orElse(null);
				if (client == null)
					return;

				// load the category index once
				if (categories == null) {
					categories = App.exec(
						"Fetch categories", () -> Api.getCategories(client));
				}

				// search for EPDs
				var epds = new ArrayList<Ec3Epd>();
				var query = searchText.getText();
				var count = spinner.getSelection();
				App.runWithProgress("Fetch EPDs", () -> {
					var response = Api.descriptors(client)
						.query(query)
						.pageSize(count)
						.get();
					epds.addAll(response.descriptors());
				}, () -> table.setInput(epds));
			};
			Controls.onSelect(searchButton, $ -> onSearch.run());
			Controls.onReturn(searchText, $ -> onSearch.run());

			form.reflow(true);
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
