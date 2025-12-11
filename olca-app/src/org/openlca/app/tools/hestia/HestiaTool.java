package org.openlca.app.tools.hestia;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
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
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;
import org.openlca.compose.ComposeBridge;
import org.openlca.core.model.ModelType;
import org.openlca.io.hestia.HestiaClient;
import org.openlca.io.hestia.SearchQuery;
import org.openlca.io.hestia.SearchResult;
import org.openlca.io.hestia.User;

import androidx.compose.ui.awt.ComposePanel;

public class HestiaTool extends SimpleFormEditor {

	private HestiaClient client;
	private User user;

	public static void open() {

		var user = ApiKeyAuth.fromCacheOrDialog(
			".hestia.json", "https://api.hestia.earth", key -> {
				var c = HestiaClient.of(key.endpoint(), key.value());
				var u = c.getCurrentUser();
				return u.isError()
					? u.wrapError("Failed to get user information")
					: Res.ok(new ApiUser(c, u.value()));
			});

		if (user.isEmpty())
			return;
		var id = AppContext.put(user.get());
		var input = new SimpleEditorInput(id, "Hestia");
		Editors.open(input, "HestiaTool");
	}

	@Override
	public void init(
		IEditorSite site, IEditorInput input) throws PartInitException {
		var inp = (SimpleEditorInput) input;
		var u = AppContext.remove(inp.id, ApiUser.class);
		if (u == null)
			throw new PartInitException("Failed to get tool data");
		setTitleImage(Icon.HESTIA.get());
		client = u.client;
		user = u.user;
		super.init(site, input);
	}

	@Override
	protected FormPage getPage() {
		return new Page(this);
	}

	record ApiUser(HestiaClient client, User user) {
	}

	private static class Page extends FormPage {

		private final HestiaClient client;
		private final User user;
		private TableViewer table;
		private Text searchText;
		private SettingsPanel settings;

		Page(HestiaTool editor) {
			super(editor, "Hestia", "Hestia");
			this.client = editor.client;
			this.user = editor.user;
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var form = UI.header(mForm, "HESTIA API Client");
			var tk = mForm.getToolkit();
			var body = UI.body(form, tk);

			var composeRoot = UI.composite(body, tk, SWT.EMBEDDED);
			var frame = SWT_AWT.new_Frame(composeRoot);
			UI.gridData(composeRoot, true, false).heightHint = 150;
			SwingUtilities.invokeLater(() -> {
				var panel = new ComposePanel();
				frame.add(panel);
				ComposeBridge.attach(panel);
				frame.validate();
			});

			userSection(tk, body);
			createConfigSection(body, tk);
			createTableSection(body, tk);
			form.addDisposeListener(e -> client.close());
		}

		private void userSection(FormToolkit tk, Composite body) {
			var section = UI.section(body, tk, "User");
			var comp = UI.sectionClient(section, tk);
			var name = UI.labeledText(comp, tk, "Name");
			Controls.set(name, user.name());
			name.setEditable(false);
			var email = UI.labeledText(comp, tk, "Email");
			Controls.set(email, user.email());
			email.setEditable(false);
			section.setExpanded(false);
			var logout = Actions.create(
				"Logout", Icon.LOGOUT.descriptor(), this::onLogout);
			Actions.bind(section, logout);
		}

		private void onLogout() {
			var q = Question.ask("Logout from HESTIA?",
				"This will delete your cached API key and close this tab."
					+ " If you want to keep it, just close the tab instead."
					+ " Do you want to logout?");
			if (!q)
				return;
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
