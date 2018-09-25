package org.openlca.app;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
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

public class SearchResultView extends SimpleFormEditor {

	public static final String ID = "SearchResultView";
	private String term;
	private List<BaseDescriptor> results;

	public static void show(String term, List<BaseDescriptor> results) {
		String resultKey = Cache.getAppCache().put(results);
		Input input = new Input(term, resultKey);
		Editors.open(input, ID);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		if (!(input instanceof Input)) {
			term = "";
			results = Collections.emptyList();
		} else {
			Input eInput = (Input) input;
			term = eInput.term;
			results = Cache.getAppCache().remove(eInput.resultKey, List.class);
			if (results == null)
				results = Collections.emptyList();
		}
	}

	@Override
	protected FormPage getPage() {
		return new Page();
	}

	private static class Input extends SimpleEditorInput {

		private String term;
		private String resultKey;

		public Input(String term, String resultKey) {
			super("search", resultKey, term);
			this.term = term;
			this.resultKey = resultKey;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return Icon.SEARCH.descriptor();
		}

		@Override
		public String getName() {
			return M.SearchResults + ": " + term;
		}
	}

	private class Page extends FormPage {

		private final int PAGE_SIZE = 50;
		private int currentPage = 0;
		private final int pageCount;

		private FormToolkit tk;
		private ScrolledForm form;
		private Composite formBody;
		private Composite pageComposite;

		public Page() {
			super(SearchResultView.this, "SearchResultView.Page", M.SearchResults);
			pageCount = (int) Math.ceil((double) results.size() / (double) PAGE_SIZE);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			form = UI.formHeader(mform, M.SearchResults + ": "
					+ term + " (" + results.size() + " " + M.Results + ")");
			tk = mform.getToolkit();
			formBody = UI.formBody(form, tk);
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
			if (end >= results.size()) {
				end = results.size() - 1;
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
