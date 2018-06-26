package org.openlca.app;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Editors;
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

	private static class Input implements IEditorInput {

		private String term;
		private String resultKey;

		public Input(String term, String resultKey) {
			this.term = term;
			this.resultKey = resultKey;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public Object getAdapter(Class adapter) {
			return null;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return Icon.SEARCH.descriptor();
		}

		@Override
		public String getName() {
			return M.SearchResults + ": " + term;
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return getName();
		}
	}

	private class Page extends FormPage {
		public Page() {
			super(SearchResultView.this, "SearchResultView.Page", M.SearchResults);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			ScrolledForm form = UI.formHeader(mform, M.SearchResults + ": "
					+ term + " (" + results.size() + " " + M.Results + ")");
			FormToolkit tk = mform.getToolkit();
			Composite body = UI.formBody(form, tk);
			UI.gridLayout(body, 1).verticalSpacing = 5;
			createItems(tk, body);
			form.reflow(true);
		}

		private void createItems(FormToolkit tk, Composite body) {
			int i = 0;
			LinkClick click = new LinkClick();
			for (BaseDescriptor d : results) {
				if (i > 1000)
					break;
				i++;
				Composite comp = tk.createComposite(body);
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
