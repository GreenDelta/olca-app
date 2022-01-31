package org.openlca.app.tools.openepd;

import java.util.ArrayList;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.components.FileChooser;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.openepd.input.ImportDialog;
import org.openlca.app.tools.openepd.model.Api;
import org.openlca.app.tools.openepd.model.Ec3CategoryTree;
import org.openlca.app.tools.openepd.model.Ec3InternalEpd;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Popup;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.jsonld.Json;

import com.google.gson.JsonObject;

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
		private Ec3CategoryTree categories;

		public Page(EpdPanel panel) {
			super(panel, "EpdPanel.Page", "openEPD");
			categories = Ec3CategoryTree.loadFromCacheFile();
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

			// search panel
			var comp = UI.sectionClient(section, tk, 1);
			var searchComp = tk.createComposite(comp);
			UI.fillHorizontal(searchComp);
			UI.gridLayout(searchComp, 4);
			var searchText = tk.createText(searchComp, "", SWT.BORDER);
			UI.fillHorizontal(searchText);
			var searchButton = tk.createButton(searchComp, "Search", SWT.NONE);
			searchButton.setImage(Icon.SEARCH.get());
			tk.createLabel(searchComp, "Max. count:");
			var spinner = new Spinner(searchComp, SWT.BORDER);
			spinner.setValues(100, 10, 1000, 0, 50, 100);
			tk.adapt(spinner);

			// category link
			if (!categories.isEmpty()) {
				var categoryLink = tk.createImageHyperlink(searchComp, SWT.NONE);
				categoryLink.setText("Loaded categories from cached file. " +
						"Click to reload categories from EC3.");
				Controls.onClick(categoryLink, $ -> {
					var client = loginPanel.login().orElse(null);
					if (client == null)
						return;
					categories = App.exec(
						"Fetch categories", () -> Api.getCategoryTree(client));
					categories.saveToCacheFile();
					categoryLink.dispose();
					var c = searchComp;
					while (c != null) {
						c.layout();
						c = c.getParent();
					}
					table.refresh(true);
				});
			}

			// descriptor table
			table = createTable(comp, loginPanel);

			// search handler
			Runnable onSearch = () -> {
				var client = loginPanel.login().orElse(null);
				if (client == null)
					return;

				// load the category index once
				if (categories.isEmpty()) {
					categories = App.exec(
						"Fetch categories", () -> Api.getCategoryTree(client));
					categories.saveToCacheFile();
				}

				// search for EPDs
				var epds = new ArrayList<Ec3InternalEpd>();
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

		private TableViewer createTable(Composite comp, LoginPanel loginPanel) {
			var table = Tables.createViewer(
				comp, "EPD", "Manufacturer", "Category", "Declared unit");
			Tables.bindColumnWidths(table, 0.25, 0.25, 0.25, 0.25);
			UI.gridData(table.getControl(), true, true);
			table.setLabelProvider(new TableLabel());

			var onImport = Actions.create(
				"Import EPD results", Icon.IMPORT.descriptor(), () -> {
					var epd = FullEpd.fetch(table, loginPanel);
					if (epd.isEmpty())
						return;
					ImportDialog.show(epd.get(), categories);
				});

			var onSaveFile = Actions.create(
				"Save as file", Icon.FILE.descriptor(), () -> {
					var epd = FullEpd.fetch(table, loginPanel);
					if (epd.isEmpty())
						return;
					var file = FileChooser.forSavingFile(
						"Save openEPD", epd.descriptor.name + ".json");
					if (file == null)
						return;
					Json.write(epd.json, file);
					Popup.info("Saved file", "Saved file " + file.getName());
				});

			Actions.bind(table, onImport, onSaveFile);
			return table;
		}

		private record FullEpd(Ec3InternalEpd descriptor, JsonObject json) {

			Ec3InternalEpd get() {
				return json != null
					? Ec3InternalEpd.fromJson(json).orElse(null)
					: null;
			}

			static FullEpd empty() {
				return new FullEpd(null, null);
			}

			boolean isEmpty() {
				return descriptor == null || json == null;
			}

			static FullEpd fetch(TableViewer table, LoginPanel loginPanel) {
				Ec3InternalEpd epd = Viewers.getFirstSelected(table);
				if (epd == null)
					return empty();
				var client = loginPanel.login().orElse(null);
				if (client == null)
					return empty();
				var json = App.exec(
					"Download EPD", () -> Api.getRawEpd(client, epd.id));
				if (json.isEmpty()) {
					MsgBox.error("Failed to download EPD " + epd.id);
					return empty();
				}
				return new FullEpd(epd, json.get());
			}
		}

		private class TableLabel extends BaseLabelProvider
			implements ITableLabelProvider {

			@Override
			public Image getColumnImage(Object obj, int col) {
				return col == 0 ? Icon.BUILDING.get() : null;
			}

			@Override
			public String getColumnText(Object obj, int col) {
				if (!(obj instanceof Ec3InternalEpd epd))
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
