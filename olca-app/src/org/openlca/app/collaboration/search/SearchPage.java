package org.openlca.app.collaboration.search;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.api.RepositoryClient;
import org.openlca.app.collaboration.model.SearchResult;
import org.openlca.app.collaboration.model.SearchResult.Dataset;
import org.openlca.app.collaboration.util.RepositoryClients;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.core.model.ModelType;
import org.openlca.git.util.TypedRefId;
import org.openlca.jsonld.ZipStore;
import org.openlca.jsonld.input.JsonImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

class SearchPage extends FormPage {

	private static final Logger log = LoggerFactory.getLogger(SearchView.class);

	private final SearchQuery query;
	private FormToolkit tk;
	private ScrolledForm form;
	private Composite formBody;
	private Section headerSection;
	private Composite headerComposite;
	private Section pageSection;
	private Composite pageComposite;
	private final List<RepositoryClient> clients = RepositoryClients.get();

	public SearchPage(SearchView view, SearchQuery query) {
		super(view, "SearchResultView.Page", M.SearchResults);
		this.query = query;
		this.query.client = !clients.isEmpty() ? clients.get(0) : null;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		form = UI.header(mform, M.SearchResults);
		tk = mform.getToolkit();
		formBody = UI.body(form, tk);
		renderPage(null);
	}

	private void renderPage(SearchResult result) {
		if (headerSection != null) {
			headerComposite.dispose();
			headerSection.dispose();
		}
		if (pageComposite != null) {
			pageComposite.dispose();
			pageSection.dispose();
		}
		form.setText(M.SearchResults + " " + (result != null ? result.resultInfo().totalCount() : 0) + " " + M.Results);
		headerSection = UI.section(formBody, tk, "Search settings");
		headerComposite = UI.sectionClient(headerSection, tk, 2);
		createRepositoryViewer();
		createModelTypeViewer();
		createQueryText();
		if (result != null) {
			pageSection = UI.section(formBody, tk, "Search results");
			pageComposite = UI.sectionClient(pageSection, tk, 1);
			createItems(result);
			renderPager(result);
		}
		form.reflow(true);
		form.getForm().setFocus();
	}

	private void createRepositoryViewer() {
		UI.label(headerComposite, tk, "Repository");
		var viewer = new AbstractComboViewer<RepositoryClient>(headerComposite) {

			@Override
			public Class<RepositoryClient> getType() {
				return RepositoryClient.class;
			}

			@Override
			protected IBaseLabelProvider getLabelProvider() {
				return new LabelProvider() {
					@Override
					public String getText(Object element) {
						var client = (RepositoryClient) element;
						return client.serverUrl + "/" + client.repositoryId;
					}
				};
			}

		};
		viewer.setInput(clients);
		viewer.select(query.client);
		viewer.addSelectionChangedListener(c -> {
			if (query.client != null) {
				query.client.close();
			}
			query.client = c;
			runSearch(1);
		});
	}

	private void createModelTypeViewer() {
		UI.label(headerComposite, tk, M.ModelType);
		var viewer = new AbstractComboViewer<ModelType>(headerComposite) {
			@Override
			public Class<ModelType> getType() {
				return ModelType.class;
			}
		};
		viewer.setNullable(true);
		viewer.setInput(ModelType.values());
		viewer.select(query.type);
		viewer.addSelectionChangedListener(type -> {
			query.type = type;
			runSearch(1);
		});
	}

	private void createQueryText() {
		UI.label(headerComposite, tk, "Query");
		var comp = UI.composite(headerComposite, tk);
		UI.gridData(comp, true, false);
		UI.gridLayout(comp, 2, 10, 0);
		var text = UI.text(comp, tk);
		Controls.set(text, query.query);
		text.addModifyListener(e -> query.query = text.getText());
		var button = tk.createButton(comp, M.Search, SWT.FLAT);
		Controls.onSelect(button, e -> runSearch(1));
	}

	private void createItems(SearchResult result) {
		var click = new LinkClick();
		for (var dataset : result.data()) {
			var comp = tk.createComposite(pageComposite);
			UI.gridData(comp, true, false);
			UI.gridLayout(comp, 1).verticalSpacing = 3;
			var header = tk.createComposite(comp);
			UI.gridData(header, true, false);
			UI.gridLayout(header, 2, 10, 0);
			var link = tk.createImageHyperlink(header, SWT.TOP);
			link.setText(dataset.name());
			link.setImage(Images.get(dataset.type()));
			link.setForeground(Colors.linkBlue());
			link.setData(getDatasetLink(dataset));
			link.addHyperlinkListener(click);
			var button = tk.createButton(header, M.Import, SWT.PUSH);
			button.setData(dataset);
			Controls.onSelect(button, this::onImport);
			var category = !Strings.isNullOrEmpty(dataset.category()) ? dataset.category() : "Uncategorized";
			var categoryLabel = tk.createLabel(comp, category);
			if (Strings.isNullOrEmpty(dataset.category())) {
				categoryLabel.setFont(UI.italicFont());
			}
			categoryLabel.setForeground(Colors.get(0, 128, 42));
			var repositoryLink = tk.createImageHyperlink(header, SWT.TOP);
			repositoryLink.setText(dataset.repositoryId());
			repositoryLink.setForeground(Colors.get(119, 0, 119));
			repositoryLink.setData(getRepositoryLink(dataset));
			repositoryLink.addHyperlinkListener(click);
		}
	}

	private void renderPager(SearchResult result) {
		var paging = result.resultInfo();
		if (paging.pageCount() < 2)
			return;
		var start = paging.currentPage() > 6 ? paging.currentPage() - 5 : 1;
		var end = start + 10;
		if (end > paging.pageCount()) {
			end = paging.pageCount();
		}
		var pager = tk.createComposite(pageComposite);
		UI.gridLayout(pager, end - start + 2);
		for (var i = start; i <= end; i++) {
			String label;
			if ((i == start && start > 1) || (i == end && end < paging.pageCount())) {
				label = "...";
			} else {
				label = Integer.toString(i);
			}
			if (i == paging.currentPage()) {
				tk.createLabel(pager, label).setFont(UI.boldFont());
				continue;
			}
			var link = tk.createHyperlink(pager, label, SWT.NONE);
			var page = i;
			Controls.onClick(link, e -> runSearch(page));
		}
	}

	private void runSearch(int page) {
		query.page = page;
		var result = Search.run(query);
		renderPage(result);
	}

	private void onImport(SelectionEvent e) {
		if (Database.get() == null) {
			MsgBox.error(M.NeedOpenDatabase);
			return;
		}
		var b = (Button) e.widget;
		var data = (Dataset) b.getData();
		var id = new TypedRefId(data.type(), data.refId());
		App.runWithProgress(M.DownloadingData, () -> {
			File tmp = null;
			ZipStore store = null;
			try {
				tmp = Files.createTempFile("cs-json-", ".zip").toFile();
				if (!query.client.downloadJson(id, tmp))
					return;
				store = ZipStore.open(tmp);
				var jsonImport = new JsonImport(store, Database.get());
				jsonImport.run();
			} catch (Exception ex) {
				log.error("Error during json import", ex);
			} finally {
				if (store != null) {
					try {
						store.close();
					} catch (IOException ex) {
						log.error("Error closing store", ex);
					}
				}
				if (tmp != null) {
					tmp.delete();
				}
			}
		}, Navigator::refresh);
	}

	private static class LinkClick extends HyperlinkAdapter {
		@Override
		public void linkActivated(HyperlinkEvent e) {
			var link = (ImageHyperlink) e.widget;
			var url = (String) link.getData();
			Desktop.browse(url);
		}
	}

	private String getDatasetLink(Dataset dataset) {
		var url = getRepositoryLink(dataset);
		url += "/dataset/" + dataset.type().name();
		url += "/" + dataset.refId();
		url += "?commitId=" + dataset.commitId();
		return url;
	}

	private String getRepositoryLink(Dataset dataset) {
		var url = query.client.serverUrl;
		url += "/" + dataset.repositoryId();
		return url;
	}

}
