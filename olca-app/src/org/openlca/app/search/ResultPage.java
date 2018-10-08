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
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.Category;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.CategoryDescriptor;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

class ResultPage extends FormPage {

	private final int PAGE_SIZE = 50;
	private final List<BaseDescriptor> rawResults;
	private final String title;

	private List<BaseDescriptor> results;
	private int currentPage = 0;
	private int pageCount;

	private FormToolkit tk;
	private ScrolledForm form;
	private Composite formBody;
	private Composite pageComposite;

	public ResultPage(SearchPage view, String title,
			List<BaseDescriptor> results) {
		super(view, "SearchResultView.Page", M.SearchResults);
		this.rawResults = results;
		this.results = rawResults;
		this.title = title;
		pageCount = (int) Math.ceil((double) results.size() / (double) PAGE_SIZE);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		form = UI.formHeader(mform, title);
		tk = mform.getToolkit();
		formBody = UI.formBody(form, tk);
		if (rawResults.size() > 10) {
			createFilter();
		}
		renderPage();
	}

	private void createFilter() {
		Composite filterComposite = tk.createComposite(formBody);
		UI.gridLayout(filterComposite, 2, 10, 10);
		Label label = tk.createLabel(filterComposite, M.Filter);
		label.setFont(UI.boldFont());
		Text text = tk.createText(filterComposite, "");
		UI.gridData(text, false, false).widthHint = 350;
		text.addModifyListener(e -> filterResults(text.getText()));
	}

	private void filterResults(String filter) {
		if (Strings.nullOrEmpty(filter)) {
			results = rawResults;
		} else {
			String term = filter.trim().toLowerCase();
			results = new ArrayList<>();
			HashMap<Long, Integer> distances = new HashMap<>();
			for (BaseDescriptor d : rawResults) {
				String n = Labels.getDisplayName(d);
				if (n == null)
					continue;
				int dist = n.toLowerCase().indexOf(term);
				if (dist < 0)
					continue;
				results.add(d);
				distances.put(d.getId(), dist);
			}
			results.sort((d1, d2) -> {
				Integer dist1 = distances.get(d1.getId());
				Integer dist2 = distances.get(d2.getId());
				if (dist1 == null || dist2 == null)
					return 0;
				return dist1 - dist2;
			});
		}
		currentPage = 0;
		pageCount = (int) Math.ceil((double) results.size() / (double) PAGE_SIZE);
		renderPage();
		return;
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
		LinkClick click = new LinkClick();
		for (BaseDescriptor d : getPageResults()) {
			Composite comp = tk.createComposite(pageComposite);
			UI.gridData(comp, true, false);
			UI.gridLayout(comp, 1).verticalSpacing = 3;
			ImageHyperlink link = tk.createImageHyperlink(comp, SWT.TOP);
			link.setText(Labels.getDisplayName(d));
			link.setImage(Images.get(d));
			link.setForeground(Colors.linkBlue());
			link.setData(d);
			link.addHyperlinkListener(click);
			renderCategory(tk, d, comp);
			renderDescription(tk, d, comp);
		}
	}

	private List<BaseDescriptor> getPageResults() {
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

	private void renderDescription(FormToolkit tk, BaseDescriptor d, Composite comp) {
		String text = Strings.cut(Labels.getDisplayInfoText(d), 400);
		if (text != null && !text.isEmpty()) {
			Label label = tk.createLabel(comp, text, SWT.WRAP);
			UI.gridData(label, false, false).widthHint = 600;
		}
	}

	private void renderCategory(FormToolkit tk, BaseDescriptor d, Composite comp) {
		if (!(d instanceof CategorizedDescriptor))
			return;
		CategorizedDescriptor cd = (CategorizedDescriptor) d;
		Long id = cd.getCategory();
		if (id == null)
			return;
		Category cat = Cache.getEntityCache().get(Category.class, id);
		if (cat == null)
			return;
		String path = CategoryPath.getFull(cat);
		Label label = tk.createLabel(comp, path);
		label.setForeground(Colors.get(0, 128, 42));
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
			Hyperlink link = tk.createHyperlink(pager,
					label, SWT.NONE);
			Controls.onClick(link, e -> {
				currentPage = pageNumber;
				renderPage();
			});
		}
	}

	private class LinkClick extends HyperlinkAdapter {
		@Override
		public void linkActivated(HyperlinkEvent e) {
			ImageHyperlink link = (ImageHyperlink) e.widget;
			Object data = link.getData();
			if (data instanceof CategoryDescriptor) {
				CategoryDescriptor d = (CategoryDescriptor) data;
				Category c = Cache.getEntityCache().get(Category.class, d.getId());
				Navigator.select(c);
			} else if (data instanceof CategorizedDescriptor) {
				App.openEditor((CategorizedDescriptor) data);
			}
		}
	}
}