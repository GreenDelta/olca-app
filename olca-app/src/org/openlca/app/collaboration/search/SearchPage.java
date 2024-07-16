package org.openlca.app.collaboration.search;

import java.util.List;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.collaboration.navigation.ServerConfigurations;
import org.openlca.app.collaboration.navigation.ServerConfigurations.ServerConfig;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.collaboration.model.Dataset;
import org.openlca.collaboration.model.SearchResult;
import org.openlca.core.model.ModelType;
import org.openlca.util.Strings;

class SearchPage extends FormPage {

	private final SearchQuery query;
	private FormToolkit tk;
	private ScrolledForm form;
	private Composite formBody;
	private Section headerSection;
	private Composite headerComposite;
	private Section pageSection;
	private Composite pageComposite;
	private SearchResults searchResults;
	private final List<ServerConfig> servers = ServerConfigurations.get();
	private ServerConfig selected;

	public SearchPage(SearchView view, SearchQuery query) {
		super(view, "SearchResultView.Page", M.SearchResults);
		this.query = query;
		this.selected = !servers.isEmpty() ? servers.get(0) : null;
		this.query.server = !servers.isEmpty() ? servers.get(0).open() : null;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		form = UI.header(mform, M.SearchResults);
		tk = mform.getToolkit();
		formBody = UI.body(form, tk);
		searchResults = new SearchResults(tk, query);
		renderPage(null);
	}

	private void renderPage(SearchResult<Dataset> result) {
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
			searchResults.render(pageComposite, result);
			renderPager(result);
		}
		form.reflow(true);
		form.getForm().setFocus();
	}

	private void createRepositoryViewer() {
		UI.label(headerComposite, tk, "Server");
		var viewer = new AbstractComboViewer<ServerConfig>(headerComposite) {

			@Override
			public Class<ServerConfig> getType() {
				return ServerConfig.class;
			}

			@Override
			protected IBaseLabelProvider getLabelProvider() {
				return new LabelProvider() {
					@Override
					public String getText(Object element) {
						var server = (ServerConfig) element;
						return server.url();
					}
				};
			}

		};
		viewer.setInput(servers);
		viewer.select(selected);
		viewer.addSelectionChangedListener(c -> {
			if (query.server != null) {
				WebRequests.execute(query.server::close);
			}
			selected = c;
			query.server = selected.open();
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
		if (!Strings.nullOrEmpty(query.type))
			viewer.select(ModelType.valueOf(query.type));
		viewer.addSelectionChangedListener(type -> {
			query.type = type != null ? type.name() : null;
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

	private void renderPager(SearchResult<Dataset> result) {
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

}
