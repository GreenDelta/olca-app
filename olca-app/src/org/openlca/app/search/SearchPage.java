package org.openlca.app.search;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.model.descriptors.BaseDescriptor;

public class SearchPage extends SimpleFormEditor {

	private Input input;
	private List<BaseDescriptor> results;

	public static void show(String term, List<BaseDescriptor> results) {
		String resultKey = Cache.getAppCache().put(results);
		Input input = new Input(term, resultKey);
		Editors.open(input, "SearchPage");
	}

	@Override
	@SuppressWarnings("unchecked")
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		if (!(input instanceof Input)) {
			this.input = new Input("", "");
			results = Collections.emptyList();
		} else {
			this.input = (Input) input;
			results = Cache.getAppCache().remove(
					this.input.resultKey, List.class);
			if (results == null) {
				results = Collections.emptyList();
			}
		}
	}

	@Override
	protected FormPage getPage() {
		String title = M.SearchResults + ": "
				+ this.input.term + " (" + results.size() + " " + M.Results + ")";
		return new ResultPage(this, title, results);
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

}
