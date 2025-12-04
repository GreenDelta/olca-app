package org.openlca.app.tools.hestia;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.AppContext;
import org.openlca.app.M;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.tools.ApiKeyAuth;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;
import org.openlca.core.model.ModelType;
import org.openlca.io.hestia.HestiaClient;
import org.openlca.io.hestia.SearchQuery;
import org.openlca.io.hestia.SearchResult;

public class HestiaTool extends SimpleFormEditor {

	private HestiaClient client;

	public static void open() {

		var client = ApiKeyAuth.fromCacheOrDialog(
			".hestia.json", "https://api.hestia.earth", key -> {
				var c = HestiaClient.of(key.endpoint(), key.value());
				// TODO: check /users/me
				return Res.ok(c);
			});

		if (client.isEmpty())
			return;
		var id = AppContext.put(client.get());
		var input = new SimpleEditorInput(id, "Hestia");
		Editors.open(input, "HestiaTool");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		var inp = (SimpleEditorInput) input;
		client = AppContext.remove(inp.id, HestiaClient.class);
		if (client == null)
			throw new PartInitException("failed to get the Hestia client");
		setTitleImage(Icon.HESTIA.get());
		super.init(site, input);
	}

	@Override
	protected FormPage getPage() {
		return new Page(this);
	}

	private static class Page extends FormPage {

		private final HestiaClient client;
		private TableViewer table;
		private Text searchText;
		private SettingsPanel settings;

		Page(HestiaTool editor) {
			super(editor, "Hestia", "Hestia");
			this.client = editor.client;
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var form = UI.header(mForm, "Hestia");
			var toolbar = form.getToolBarManager();
			toolbar.add(Actions.create(
				"Logout", Icon.LOGOUT.descriptor(), this::onLogout));
			toolbar.update(true);

			var tk = mForm.getToolkit();
			var body = UI.body(form, tk);

			createConfigSection(body, tk);
			createTableSection(body, tk);
		}

		private void onLogout() {
			var file = new File(Workspace.root(), ".hestia.json");
			if (file.exists()) {
				try {
					Files.delete(file.toPath());
				} catch (Exception e) {
					ErrorReporter.on("Failed to delete API key", e);
				}
			}
			getEditor().close(false);
		}

		private void createConfigSection(Composite body, FormToolkit tk) {
			var section = UI.section(body, tk, M.Search);
			var comp = UI.sectionClient(section, tk, 1);

			var searchComp = tk.createComposite(comp);
			UI.fillHorizontal(searchComp);
			var grid = UI.gridLayout(searchComp, 2);
			grid.marginWidth = 0;
			grid.marginHeight = 0;

			searchText = tk.createText(searchComp, "", SWT.BORDER);
			UI.fillHorizontal(searchText);
			searchText.setMessage("Search for datasets...");

			var searchButton = tk.createButton(searchComp, M.Search, SWT.NONE);
			searchButton.setImage(Icon.SEARCH.get());
			Controls.onSelect(searchButton, e -> runSearch());
			searchText.addTraverseListener(e -> {
				if (e.detail == SWT.TRAVERSE_RETURN) {
					runSearch();
				}
			});

			settings = new SettingsPanel(searchComp, tk);

		}

		private void createTableSection(Composite body, FormToolkit tk) {
			var section = UI.section(body, tk, M.SearchResults);
			UI.gridData(section, true, true);
			var comp = UI.sectionClient(section, tk, 1);

			table = Tables.createViewer(comp, "Cycle", "ID");
			UI.gridData(table.getControl(), true, true);
			Tables.bindColumnWidths(table, 0.6, 0.4);
			table.setLabelProvider(new SearchResultLabel());

			var importAction = Actions.create(
				M.ImportSelected, Icon.IMPORT.descriptor(), this::runImport);
			var previewAction = Actions.create(
				"Show cycle", Icon.HESTIA.descriptor(), () -> {
					SearchResult r = Viewers.getFirstSelected(table);
					if (r != null) {
						CyclePreviewDialog.show(client, r.id());
					}
				});
			Actions.bind(table, previewAction, importAction);
		}

		private void runSearch() {
			var query = Strings.nullIfBlank(searchText.getText());
			if (query == null) {
				table.setInput(new ArrayList<>());
				return;
			}
			var agg = settings.searchAggregated();
			var count = settings.numberOfResults();

			var ref = new AtomicReference<Res<List<SearchResult>>>();
			App.runWithProgress("Searching Hestia...", () -> {
				var res = client.search(new SearchQuery(count, query, agg));
				ref.set(res);
			}, () -> {
				var res = ref.get();
				if (res.isError()) {
					MsgBox.error("Search failed", res.error());
				} else {
					table.setInput(res.value());
				}
			});
		}

		private void runImport() {
			List<SearchResult> selected = Viewers.getAllSelected(table);
			if (selected.isEmpty())
				return;

			ImportDialog.show(client, selected);
		}
	}

	private static class SearchResultLabel extends BaseLabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return col == 0 ? Images.get(ModelType.PROCESS) : null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			return obj instanceof SearchResult r
				? col == 0 ? r.name() : r.id()
				: null;
		}
	}
}
