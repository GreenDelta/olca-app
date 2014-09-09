package org.openlca.app;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Editors;
import org.openlca.app.util.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.Category;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchResultView extends FormEditor {

	public static final String ID = "SearchResultView";
	private Logger log = LoggerFactory.getLogger(getClass());
	private String term;
	private List<BaseDescriptor> results;

	public static void show(String term, List<BaseDescriptor> results) {
		String resultKey = Cache.getAppCache().put(results);
		Input input = new Input(term, resultKey);
		Editors.open(input, ID);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
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
	protected void addPages() {
		try {
			addPage(new Page());
		} catch (Exception e) {
			log.error("failed to add editor page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
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
			return ImageType.SEARCH_ICON.getDescriptor();
		}

		@Override
		public String getName() {
			return Messages.SearchResults + ": " + term;
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
			super(SearchResultView.this, "SearchResultView.Page",
					Messages.SearchResults);
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			ScrolledForm form = UI.formHeader(managedForm,
					Messages.SearchResults + ": " + term + " (" + results.size()
							+ Messages.Results + ")");
			FormToolkit toolkit = managedForm.getToolkit();
			Composite body = UI.formBody(form, toolkit);
			UI.gridLayout(body, 1).verticalSpacing = 5;
			createItems(toolkit, body);
			form.reflow(true);
		}

		private void createItems(FormToolkit toolkit, Composite body) {
			int i = 0;
			LinkClick click = new LinkClick();
			for (BaseDescriptor descriptor : results) {
				if (i > 1000)
					break;
				i++;
				Composite composite = toolkit.createComposite(body);
				UI.gridData(composite, true, false);
				UI.gridLayout(composite, 1).verticalSpacing = 3;
				ImageHyperlink link = toolkit.createImageHyperlink(composite,
						SWT.CENTER);
				link.setText(Labels.getDisplayName(descriptor));
				link.setImage(Images.getIcon(descriptor.getModelType()));
				link.setForeground(Colors.getLinkBlue());
				link.setData(descriptor);
				link.addHyperlinkListener(click);
				renderCategory(toolkit, descriptor, composite);
				renderDescription(toolkit, descriptor, composite);
			}
		}

		private void renderDescription(FormToolkit toolkit,
				BaseDescriptor descriptor, Composite composite) {
			String description = Strings.cut(
					Labels.getDisplayInfoText(descriptor), 400);
			if (description != null && !description.isEmpty()) {
				Label descriptionLabel = toolkit.createLabel(composite,
						description, SWT.WRAP);
				UI.gridData(descriptionLabel, false, false).widthHint = 600;
			}
		}

		private void renderCategory(FormToolkit toolkit,
				BaseDescriptor descriptor, Composite composite) {
			if (!(descriptor instanceof CategorizedDescriptor))
				return;
			CategorizedDescriptor cd = (CategorizedDescriptor) descriptor;
			Long categoryId = cd.getCategory();
			if (categoryId == null)
				return;
			Category cat = Cache.getEntityCache().get(Category.class,
					categoryId);
			if (cat == null)
				return;
			String catPath = CategoryPath.getFull(cat);
			Label label = toolkit.createLabel(composite, catPath);
			label.setForeground(Colors.getColor(0, 128, 42));
		}
	}

	private class LinkClick extends HyperlinkAdapter {
		@Override
		public void linkActivated(HyperlinkEvent e) {
			ImageHyperlink link = (ImageHyperlink) e.widget;
			Object data = link.getData();
			if (data instanceof BaseDescriptor) {
				App.openEditor((BaseDescriptor) data);
			}
		}
	}
}
