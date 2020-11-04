package org.openlca.app.cloud.ui.search;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.preferences.CloudConfiguration;
import org.openlca.app.cloud.ui.preferences.CloudConfigurations;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.cloud.model.data.DatasetEntry;
import org.openlca.cloud.model.data.FileReference;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.openlca.core.model.ModelType;
import org.openlca.jsonld.ZipStore;
import org.openlca.jsonld.input.JsonImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.greendelta.search.wrapper.SearchResult;
import com.greendelta.search.wrapper.SearchResult.ResultInfo;

class SearchPage extends FormPage {

	private static final Logger log = LoggerFactory.getLogger(SearchView.class);

	private SearchQuery query;
	private SearchResult<DatasetEntry> result;

	private FormToolkit tk;
	private ScrolledForm form;
	private Composite formBody;
	private Section headerSection;
	private Composite headerComposite;
	private Section pageSection;
	private Composite pageComposite;
	private AbstractComboViewer<String> repositoryViewer;

	public SearchPage(SearchView view, SearchQuery query, SearchResult<DatasetEntry> result) {
		super(view, "SearchResultView.Page", M.SearchResults);
		this.result = result;
		this.query = query;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		form = UI.formHeader(mform, M.SearchResults);
		tk = mform.getToolkit();
		formBody = UI.formBody(form, tk);
		renderPage();
	}

	private void renderPage() {
		if (headerSection != null) {
			headerComposite.dispose();
			headerSection.dispose();
		}
		if (pageComposite != null) {
			pageComposite.dispose();
			pageSection.dispose();
		}
		form.setText(M.SearchResults + ": " + query + " (" + result.resultInfo.totalCount + " " + M.Results + ")");
		headerSection = UI.section(formBody, tk, "Search settings");
		pageSection = UI.section(formBody, tk, "Search results");
		headerComposite = UI.sectionClient(headerSection, tk, 2);
		pageComposite = UI.sectionClient(pageSection, tk, 1);
		createConfigurationViewer();
		createRepositoryViewer();
		createModelTypeViewer();
		createQueryText();
		createItems();
		renderPager();
		form.reflow(true);
		form.getForm().setFocus();
	}

	private void createConfigurationViewer() {
		UI.formLabel(headerComposite, tk, "Collaboration server configuration");
		AbstractComboViewer<CloudConfiguration> viewer = new AbstractComboViewer<CloudConfiguration>(headerComposite) {

			@Override
			public Class<CloudConfiguration> getType() {
				return CloudConfiguration.class;
			}

		};
		viewer.setInput(CloudConfigurations.get());
		viewer.select(query.getConfiguration());
		viewer.addSelectionChangedListener(c -> {
			if (query.getClient() != null) {
				try {
					query.getClient().logout();
				} catch (WebRequestException e) {
					log.error("Error closing cs client", e);
				}
			}
			query.setConfiguration(c);
			try {
				repositoryViewer.setInput(query.getClient().listRepositories());
			} catch (WebRequestException e) {
				log.error("Error loading repositories", e);
			}
			runSearch(1);
		});
	}

	private void createRepositoryViewer() {
		UI.formLabel(headerComposite, tk, M.Repository);
		repositoryViewer = new AbstractComboViewer<String>(headerComposite) {
			@Override
			public Class<String> getType() {
				return String.class;
			}
		};
		repositoryViewer.setNullable(true);
		try {
			repositoryViewer.setInput(query.getClient().listRepositories());
		} catch (WebRequestException e) {
			log.error("Error loading repositories", e);
		}
		repositoryViewer.select(query.getClient().getConfig().repositoryId);
		repositoryViewer.addSelectionChangedListener(repoId -> {
			query.getClient().getConfig().repositoryId = repoId;
			runSearch(1);
		});
	}

	private void createModelTypeViewer() {
		UI.formLabel(headerComposite, tk, M.ModelType);
		AbstractComboViewer<ModelType> viewer = new AbstractComboViewer<ModelType>(headerComposite) {
			@Override
			public Class<ModelType> getType() {
				return ModelType.class;
			}

			@Override
			protected IBaseLabelProvider getLabelProvider() {
				return new BaseLabelProvider();
			}
		};
		viewer.setNullable(true);
		viewer.setInput(ModelType.categorized());
		viewer.select(query.type);
		viewer.addSelectionChangedListener(type -> {
			query.type = type;
			runSearch(1);
		});
	}

	private void createQueryText() {
		UI.formLabel(headerComposite, tk, "Term");
		Composite comp = UI.formComposite(headerComposite);
		UI.gridData(comp, true, true);
		UI.gridLayout(comp, 2, 0, 0);
		Text text = UI.formText(comp, tk, null);
		Controls.set(text, query.query);
		text.addModifyListener(e -> query.query = text.getText());
		Button button = tk.createButton(comp, M.Search, SWT.FLAT);
		Controls.onSelect(button, e -> runSearch(1));
	}

	private void createItems() {
		LinkClick click = new LinkClick();
		for (DatasetEntry d : result.data) {
			Composite comp = tk.createComposite(pageComposite);
			UI.gridData(comp, true, false);
			UI.gridLayout(comp, 1).verticalSpacing = 3;
			Composite header = tk.createComposite(comp);
			UI.gridData(header, true, false);
			UI.gridLayout(header, 2, 10, 0);
			ImageHyperlink link = tk.createImageHyperlink(header, SWT.TOP);
			link.setText(d.name);
			link.setImage(Images.get(d.type));
			link.setForeground(Colors.linkBlue());
			link.setData(getDatasetLink(d));
			link.addHyperlinkListener(click);
			Button button = tk.createButton(header, M.Import, SWT.PUSH);
			button.setData(d);
			Controls.onSelect(button, this::onImport);
			String category = !Strings.isNullOrEmpty(d.category) ? d.category : "Uncategorized";
			Label categoryLabel = tk.createLabel(comp, category);
			if (Strings.isNullOrEmpty(d.category)) {
				categoryLabel.setFont(UI.italicFont());
			}
			categoryLabel.setForeground(Colors.get(0, 128, 42));
			ImageHyperlink repositoryLink = tk.createImageHyperlink(header, SWT.TOP);
			repositoryLink.setText(d.name);
			repositoryLink.setForeground(Colors.get(119, 0, 119));
			repositoryLink.setData(getRepositoryLink(d));
			repositoryLink.addHyperlinkListener(click);
		}
	}

	private void renderPager() {
		ResultInfo paging = result.resultInfo;
		if (paging.pageCount < 2)
			return;
		int start = paging.currentPage > 6 ? paging.currentPage - 5 : 1;
		int end = start + 10;
		if (end > paging.pageCount) {
			end = paging.pageCount;
		}
		Composite pager = tk.createComposite(pageComposite);
		UI.gridLayout(pager, end - start + 2);
		for (int i = start; i <= end; i++) {
			String label;
			if ((i == start && start > 1) || (i == end && end < paging.pageCount)) {
				label = "...";
			} else {
				label = Integer.toString(i);
			}
			if (i == paging.currentPage) {
				tk.createLabel(pager, label).setFont(UI.boldFont());
				continue;
			}
			Hyperlink link = tk.createHyperlink(pager, label, SWT.NONE);
			int page = i;
			Controls.onClick(link, e -> runSearch(page));
		}
	}

	private void runSearch(int page) {
		query.page = page;
		Search search = new Search(query);
		App.runWithProgress(M.Searching, search, () -> {
			result = search.result;
			renderPage();
		});
	}

	private void onImport(SelectionEvent e) {
		if (Database.get() == null) {
			MsgBox.error(M.NeedOpenDatabase);
			return;
		}
		Button b = (Button) e.widget;
		DatasetEntry data = (DatasetEntry) b.getData();
		query.getClient().getConfig().repositoryId = data.repositoryId;
		Set<FileReference> requestData = Collections.singleton(FileReference.from(data.type, data.refId));
		App.runWithProgress(M.DownloadingData, () -> {
			try {
				File tmp = query.getClient().downloadJson(requestData);
				ZipStore store = ZipStore.open(tmp);
				JsonImport jsonImport = new JsonImport(store, Database.get());
				jsonImport.run();
				store.close();
				tmp.delete();
			} catch (Exception e1) {
				log.error("Error during json import", e1);
			}
		}, () -> Navigator.refresh());
	}

	private class LinkClick extends HyperlinkAdapter {
		@Override
		public void linkActivated(HyperlinkEvent e) {
			ImageHyperlink link = (ImageHyperlink) e.widget;
			String url = (String) link.getData();
			Desktop.browse(url);
		}
	}

	private String getDatasetLink(DatasetEntry entry) {
		String url = getRepositoryLink(entry);
		url += "/dataset/" + entry.type.name();
		url += "/" + entry.refId;
		url += "?commitId=" + entry.commitId;
		return url;
	}

	private String getRepositoryLink(DatasetEntry entry) {
		String url = query.getConfiguration().getUrl();
		url += "/" + entry.repositoryId;
		return url;
	}

}