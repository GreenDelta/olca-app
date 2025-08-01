package org.openlca.app.tools.openepd;

import java.util.ArrayList;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.openepd.input.ImportDialog;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Popup;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.io.openepd.Api;
import org.openlca.io.openepd.Ec3Client;
import org.openlca.io.openepd.Ec3EpdInfo;
import org.openlca.io.openepd.EpdDoc;
import org.openlca.jsonld.Json;
import org.openlca.util.Strings;

import com.google.gson.JsonObject;

public class EpdPanel extends SimpleFormEditor {

	public static void open() {
		Editors.open(new SimpleEditorInput("EpdPanel", "openEPD"), "EpdPanel");
	}

	@Override
	protected FormPage getPage() {
		setTitleImage(Icon.BUILDING.get());
		return new Page(this);
	}

	private static class Page extends FormPage {

		private TableViewer table;

		public Page(EpdPanel panel) {
			super(panel, "EpdPanel.Page", "openEPD");
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var form = UI.header(mForm, "Building Transparency - openEPD",
				Icon.EC3_WIZARD.get());
			var tk = mForm.getToolkit();
			var body = UI.body(form, tk);

			// login
			var loginPanel = LoginPanel.create(body, tk);

			// direct download
			var downloadComp = UI.formSection(body, tk, M.DirectDownload);
			UI.gridLayout(downloadComp, 3);
			UI.label(downloadComp, tk, M.UrlOrId);
			var urlText = UI.labeledText(downloadComp, "", SWT.BORDER);
			UI.fillHorizontal(urlText);
			var downloadBtn = UI.button(downloadComp, tk, M.Download);
			Controls.onSelect(downloadBtn, $ -> {
				var client = loginPanel.login().orElse(null);
				if (client == null)
					return;
				directDownload(client, urlText.getText());
			});

			// search panel
			var section = UI.section(body, tk, M.FindEpds);
			UI.gridData(section, true, true);
			var comp = UI.sectionClient(section, tk, 1);
			var searchComp = UI.composite(comp, tk);
			UI.fillHorizontal(searchComp);
			UI.gridLayout(searchComp, 4);
			var searchText = UI.emptyText(searchComp, tk, SWT.BORDER);
			UI.fillHorizontal(searchText);
			var searchButton = UI.button(searchComp, tk, M.Search);
			searchButton.setImage(Icon.SEARCH.get());
			UI.label(searchComp, tk, M.MaxCount);
			var spinner = UI.spinner(searchComp, tk);
			spinner.setValues(100, 10, 1000, 0, 50, 100);

			// descriptor table
			table = createTable(comp, loginPanel);

			// search handler
			Runnable onSearch = () -> {
				var client = loginPanel.login().orElse(null);
				if (client == null)
					return;

				// search for EPDs
				var epds = new ArrayList<Ec3EpdInfo>();
				var query = searchText.getText();
				var count = spinner.getSelection();
				App.runWithProgress(M.FetchEpdsDots, () -> {
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
				comp, "EPD", M.Manufacturer, M.Category, M.DeclaredUnit);
			Tables.bindColumnWidths(table, 0.25, 0.25, 0.25, 0.25);
			UI.gridData(table.getControl(), true, true);
			table.setLabelProvider(new TableLabel());

			var onImport = Actions.create(
				M.ImportEpdResults, Icon.IMPORT.descriptor(), () -> {
					var epd = FullEpd.fetch(table, loginPanel);
					if (epd.isEmpty())
						return;
					ImportDialog.show(epd.get());
				});

			var onSaveFile = Actions.create(
				M.SaveAsFile, Icon.FILE.descriptor(), () -> {
					var epd = FullEpd.fetch(table, loginPanel);
					if (epd.isEmpty())
						return;
					var file = FileChooser.forSavingFile(M.SaveOpenEpd,
							epd.descriptor.epdId + ".json");
					if (file == null)
						return;
					Json.write(epd.json, file);
					Popup.info(M.SavedFile, M.SavedFile + " - " + file.getName());
				});

			Actions.bind(table, onImport, onSaveFile);
			return table;
		}

		private void directDownload(Ec3Client client, String url) {
			if (Strings.nullOrEmpty(url)) {
				MsgBox.error(M.NoUrlOrIdProvided);
				return;
			}

			// we assume that the last non-empty part of the provided
			// URL is the ID of the EPD.
			var parts = url.split("/");
			String last = null;
			for (int i = parts.length - 1; i >= 0; i--) {
				var part = parts[i].trim();
				if (!part.isEmpty()) {
					last = part;
					break;
				}
			}
			if (last == null) {
				MsgBox.error(M.NoValidUrlOrEpdIdProvided);
				return;
			}

			var id = last;
			var json = App.exec(
				M.DownloadEpd + " - " + id, () -> Api.getRawEpd(client, id));
			if (json.isEmpty()) {
				MsgBox.error(M.CouldNotDownloadEpdId + " (" + id + ")");
				return;
			}
			var epd = EpdDoc.fromJson(json.get()).orElse(null);
			ImportDialog.show(epd);
		}

		private record FullEpd(Ec3EpdInfo descriptor, JsonObject json) {

			EpdDoc get() {
				return json != null
					? EpdDoc.fromJson(json).orElse(null)
					: null;
			}

			static FullEpd empty() {
				return new FullEpd(null, null);
			}

			boolean isEmpty() {
				return descriptor == null || json == null;
			}

			static FullEpd fetch(TableViewer table, LoginPanel loginPanel) {
				Ec3EpdInfo info = Viewers.getFirstSelected(table);
				if (info == null)
					return empty();
				var client = loginPanel.login().orElse(null);
				if (client == null)
					return empty();
				var json = App.exec(
					M.DownloadEpdDots, () -> Api.getRawEpd(client, info.epdId));
				if (json.isEmpty()) {
					MsgBox.error(M.FailedToDownloadEpd + " (" + info.epdId + ")");
					return empty();
				}
				return new FullEpd(info, json.get());
			}
		}

		private static class TableLabel extends BaseLabelProvider
			implements ITableLabelProvider {

			@Override
			public Image getColumnImage(Object obj, int col) {
				return col == 0 ? Icon.BUILDING.get() : null;
			}

			@Override
			public String getColumnText(Object obj, int col) {
				if (!(obj instanceof Ec3EpdInfo epd))
					return null;
				return switch (col) {
					case 0 -> epd.name;
					case 1 -> epd.manufacturer != null
						? epd.manufacturer.name
						: null;
					case 2 -> epd.category != null
						? epd.category.openEpd
						: null;
					case 3 -> epd.declaredUnit != null
						? epd.declaredUnit
						: null;
					default -> null;
				};
			}
		}
	}
}
