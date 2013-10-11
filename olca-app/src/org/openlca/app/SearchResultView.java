package org.openlca.app;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.db.Cache;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.openlca.core.model.descriptors.BaseDescriptor;
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
			return "Search results for " + term;
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
					"Search results");
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			UI.formHeader(managedForm, "Search results for '" + term + "'");
		}
	}
}
