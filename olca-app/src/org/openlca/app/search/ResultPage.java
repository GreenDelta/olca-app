package org.openlca.app.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.AppContext;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.Category;
import org.openlca.core.model.descriptors.CategoryDescriptor;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.util.Categories;
import org.openlca.util.Strings;

class ResultPage extends FormPage {

	private final int PAGE_SIZE = 50;
	private final List<Descriptor> rawResults;
	private final String title;
	private final Categories.PathBuilder categories;

	private List<Descriptor> results;
	private int currentPage = 0;
	private int pageCount;

	private FormToolkit tk;
	private ScrolledForm form;
	private Composite formBody;
	private Composite pageComposite;
	private Label filterLabel;

	public ResultPage(SearchPage view, String title, List<Descriptor> results) {
		super(view, "SearchResultView.Page", M.SearchResults);
		this.rawResults = results;
		this.results = rawResults;
		this.title = title;
		pageCount = (int) Math.ceil((double) results.size() / (double) PAGE_SIZE);
		categories = Categories.pathsOf(Database.get());
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		this.form = UI.header(form, title);
		tk = form.getToolkit();
		formBody = UI.body(this.form, tk);
		if (rawResults.size() > 10) {
			createFilter();
		}
		renderPage();
	}

	private void createFilter() {
		var filterComposite = UI.composite(formBody, tk);
		UI.gridLayout(filterComposite, 2, 10, 10);
		filterLabel = UI.label(filterComposite, tk, "");
		setFilterLabelText();
		filterLabel.setFont(UI.boldFont());
		Text text = UI.emptyText(filterComposite, tk);
		UI.gridData(text, false, false).widthHint = 350;
		text.addModifyListener(e -> filterResults(text.getText()));
	}

	private void setFilterLabelText() {
		filterLabel.setText(M.Filter + " (" + results.size() + ")");
	}

	private void filterResults(String filter) {
		if (Strings.nullOrEmpty(filter)) {
			results = rawResults;
		} else {
			String term = filter.trim().toLowerCase();
			results = new ArrayList<>();
			HashMap<Long, Integer> distances = new HashMap<>();
			for (var d : rawResults) {
				String n = Labels.name(d);
				if (n == null)
					continue;
				int dist = n.toLowerCase().indexOf(term);
				if (dist < 0)
					continue;
				results.add(d);
				distances.put(d.id, dist);
			}
			results.sort((d1, d2) -> {
				Integer dist1 = distances.get(d1.id);
				Integer dist2 = distances.get(d2.id);
				if (dist1 == null || dist2 == null)
					return 0;
				return dist1 - dist2;
			});
		}
		setFilterLabelText();
		currentPage = 0;
		pageCount = (int) Math.ceil((double) results.size() / (double) PAGE_SIZE);
		renderPage();
	}

	private void renderPage() {
		if (pageComposite != null) {
			pageComposite.dispose();
		}
		pageComposite = tk.createComposite(formBody);
		UI.gridData(pageComposite, true, true);
		UI.gridLayout(pageComposite, 1, 5, 5);
		createItems();
		renderPager();
		form.reflow(true);
		form.getForm().setFocus();
	}

	private void createItems() {
		var click = new LinkClick();
		for (var d : getPageResults()) {
			var comp = tk.createComposite(pageComposite);
			UI.gridData(comp, true, false);
			UI.gridLayout(comp, 1).verticalSpacing = 3;
			var link = tk.createImageHyperlink(comp, SWT.TOP);
			link.setText(Labels.name(d));
			link.setImage(Images.get(d));
			link.setForeground(Colors.linkBlue());
			link.setData(d);
			link.addHyperlinkListener(click);
			renderCategory(tk, comp, d);
			renderTags(tk, comp, d);
		}
	}

	private List<Descriptor> getPageResults() {
		if (results == null || results.isEmpty())
			return Collections.emptyList();
		int start = currentPage * 50;
		if (start >= results.size())
			return Collections.emptyList();
		int end = start + 50;
		if (end > results.size()) {
			end = results.size();
		}
		return results.subList(start, end);
	}

	private void renderCategory(FormToolkit tk, Composite comp, Descriptor d) {
		if (!(d instanceof RootDescriptor e) || e.category == null)
			return;
		var path = categories.pathOf(e.category);
		var label = tk.createLabel(comp, path);
		label.setForeground(Colors.get(0, 128, 42));
	}

	private void renderTags(FormToolkit tk, Composite parent, Descriptor d) {
		if (Strings.nullOrEmpty(d.tags))
			return;
		var tags = d.tags.split(",");
		if (tags.length == 0)
			return;
		var comp = tk.createComposite(parent);
		UI.gridLayout(comp, tags.length, 5, 0);
		for (var tag : tags) {
			tk.createLabel(comp, "#" + tag).setForeground(Colors.darkGray());
		}
	}

	private void renderPager() {
		if (pageCount < 2)
			return;
		int start = currentPage > 5 ? currentPage - 5 : 0;
		int end = start + 10;
		if (end >= pageCount) {
			end = pageCount - 1;
		}
		Composite pager = tk.createComposite(pageComposite);
		UI.gridLayout(pager, end - start + 1);
		for (int i = start; i <= end; i++) {
			String label;
			if ((i == start && start > 0)
					|| (i == end && end < (pageCount - 1))) {
				label = "...";
			} else {
				label = Integer.toString(i + 1);
			}
			if (i == currentPage) {
				tk.createLabel(pager, label)
						.setFont(UI.boldFont());
				continue;
			}
			int pageNumber = i;
			var link = tk.createHyperlink(pager, label, SWT.NONE);
			Controls.onClick(link, e -> {
				currentPage = pageNumber;
				renderPage();
			});
		}
	}

	private static class LinkClick extends HyperlinkAdapter {
		@Override
		public void linkActivated(HyperlinkEvent e) {
			var link = (ImageHyperlink) e.widget;
			var data = link.getData();
			if (data instanceof CategoryDescriptor d) {
				var c = AppContext.getEntityCache().get(Category.class, d.id);
				Navigator.select(c);
			} else if (data instanceof RootDescriptor d) {
				App.open(d);
			}
		}
	}
}
