package org.openlca.app.tools.hestia;

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
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.tools.ApiKeyAuth;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.wizards.io.ImportLogDialog;
import org.openlca.core.model.ModelType;
import org.openlca.io.hestia.HestiaClient;
import org.openlca.io.hestia.HestiaImport;
import org.openlca.io.hestia.SearchQuery;
import org.openlca.io.hestia.SearchResult;
import org.openlca.util.Res;
import org.openlca.util.Strings;

public class HestiaTool extends SimpleFormEditor {

	private HestiaClient client;

	public static void open() {

		var client = ApiKeyAuth.fromCacheOrDialog(
				".hestia.json", "https://api.hestia.earth", key -> {
					var c = HestiaClient.of(key.endpoint(), key.value());
					// TODO: check /users/me
					return Res.of(c);
				}
		);

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
			var tk = mForm.getToolkit();
			var body = UI.body(form, tk);

			createConfigSection(body, tk);
			createTableSection(body, tk);
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
			Actions.bind(table, importAction);
		}

		private void runSearch() {
			var query = Strings.nullIfEmpty(searchText.getText());
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
				if (res.hasError()) {
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

			var db = Database.get();
			if (db == null) {
				MsgBox.info(M.NoDatabaseOpened, M.NeedOpenDatabase);
				return;
			}

			var imp = new HestiaImport(client, db, settings.flowMap());
			var log = imp.log();
			App.runWithProgress(
					"Importing data sets...",
					() -> {
						for (var r : selected) {
							var res = imp.importCycle(r.id());
							if (res.hasError()) {
								log.error("failed to import " + r.name() + ": " + res.error());
							} else {
								log.imported(res.value());
							}
						}
					},
					() -> {
						ImportLogDialog.show("Import finished", log);
						Navigator.refresh();
						AppContext.evictAll();
					});
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
