package org.openlca.app.collaboration.search;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.M;
import org.openlca.app.collaboration.util.Datasets;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.collaboration.model.Dataset;
import org.openlca.collaboration.model.Dataset.Repo;
import org.openlca.collaboration.model.Dataset.Version;
import org.openlca.collaboration.model.SearchResult;
import org.openlca.core.model.ModelType;
import org.openlca.util.Strings;

class SearchResults {

	private final FormToolkit tk;
	private final SearchQuery query;

	SearchResults(FormToolkit tk, SearchQuery query) {
		this.tk = tk;
		this.query = query;
	}

	void render(Composite parent, SearchResult<Dataset> result) {
		for (var dataset : result.data()) {
			var firstVersion = dataset.versions().get(0);
			var firstRepo = firstVersion.repos().get(0);
			var comp = tk.createComposite(parent);
			UI.gridData(comp, true, false);
			UI.gridLayout(comp, 1).verticalSpacing = 3;
			renderDatasetLink(comp, dataset, firstVersion, firstRepo);
			renderCategoryLabel(comp, firstVersion);
			renderRepos(comp, dataset, firstVersion);
			renderOtherVersion(comp, dataset);
		}
	}

	private void renderDatasetLink(Composite parent, Dataset dataset, Version version, Repo repo) {
		var header = tk.createComposite(parent);
		UI.gridData(header, true, false);
		UI.gridLayout(header, 2, 1, 0);
		var link = tk.createImageHyperlink(header, SWT.TOP);
		link.setText(version.name());
		link.setImage(Images.get(ModelType.valueOf(dataset.type())));
		link.setForeground(Colors.linkBlue());
		link.setData(getDatasetLink(dataset, repo));
		link.addHyperlinkListener(new LinkClick());
		var button = tk.createButton(header, M.ImportDots, SWT.PUSH);
		button.setData(new Object[] { dataset, repo });
		Controls.onSelect(button, this::onImport);
	}

	private void renderCategoryLabel(Composite parent, Version version) {
		var category = Strings.isNotBlank(version.category()) ? version.category() : "Uncategorized";
		var label = tk.createLabel(parent, category);
		if (Strings.isBlank(version.category())) {
			label.setFont(UI.italicFont());
		}
		label.setForeground(Colors.get(0, 128, 42));
	}

	private void renderRepos(Composite parent, Dataset dataset, Version version) {
		var comp = tk.createComposite(parent);
		UI.gridData(comp, true, false);
		UI.gridLayout(comp, version.repos().size() + 1, 10, 0);
		var label = version.repos().size() > 1 ? "Found in repositories" : "Found in repository";
		tk.createLabel(comp, label);
		for (var i = 0; i < version.repos().size(); i++) {
			var repo = version.repos().get(i);
			var link = tk.createImageHyperlink(comp, SWT.TOP);
			var text = repo.path();
			if (i != version.repos().size() - 1) {
				text += ",";
			}
			link.setText(text);
			link.setForeground(Colors.get(119, 0, 119));
			link.setData(getDatasetLink(dataset, repo));
			link.addHyperlinkListener(new LinkClick());
		}
	}

	private void renderOtherVersion(Composite parent, Dataset dataset) {
		var otherVersions = 0;
		for (var i = 1; i < dataset.versions().size(); i++) {
			otherVersions += dataset.versions().get(i).repos().size();
		}
		if (otherVersions == 0)
			return;
		var comp = tk.createComposite(parent);
		UI.gridData(comp, true, false);
		UI.gridLayout(comp, otherVersions + 1, 10, 0);
		var versionsLabel = dataset.versions().size() > 2 ? "Other versions found in" : "Other version found in";
		tk.createLabel(comp, versionsLabel);
		for (var i = 1; i < dataset.versions().size(); i++) {
			var version = dataset.versions().get(i);
			for (var j = 0; j < version.repos().size(); j++) {
				var repo = version.repos().get(j);
				var link = tk.createImageHyperlink(comp, SWT.TOP);
				var text = repo.path();
				if (i != dataset.versions().size() - 1 || j != version.repos().size() - 1) {
					text += ",";
				}
				link.setText(text);
				link.setForeground(Colors.get(119, 0, 119));
				link.setData(getDatasetLink(dataset, repo));
				link.addHyperlinkListener(new LinkClick());
			}
		}
	}

	private String getDatasetLink(Dataset dataset, Repo repo) {
		var url = getRepositoryLink(repo);
		url += "/dataset/" + dataset.type();
		url += "/" + dataset.refId();
		url += "?commitId=" + repo.commitId();
		return url;
	}

	private String getRepositoryLink(Repo repo) {
		var url = query.client.url;
		url += "/" + repo.path();
		return url;
	}

	private void onImport(SelectionEvent e) {
		if (Database.get() == null) {
			MsgBox.error(M.NeedOpenDatabase);
			return;
		}
		var b = (Button) e.widget;
		var data = (Object[]) b.getData();
		var dataset = (Dataset) data[0];
		var repo = (Repo) data[1];
		Datasets.download(query.client, repo.path(), dataset.type(), dataset.refId());
	}

	private static class LinkClick extends HyperlinkAdapter {
		@Override
		public void linkActivated(HyperlinkEvent e) {
			var link = (ImageHyperlink) e.widget;
			var url = (String) link.getData();
			Desktop.browse(url);
		}
	}
}
