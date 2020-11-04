package org.openlca.app.cloud.ui.search;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.M;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.cloud.model.data.DatasetEntry;

import com.greendelta.search.wrapper.SearchResult;

public class SearchView extends SimpleFormEditor {

	private Input input;

	public static void show(SearchQuery query, SearchResult<DatasetEntry> result) {
		Editors.open(new Input(query, result), "SearchView");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		this.input = (Input) input;
	}

	@Override
	protected FormPage getPage() {
		return new SearchPage(this, input.query, input.result);
	}

	private static class Input extends SimpleEditorInput {

		private final SearchQuery query;
		private final SearchResult<DatasetEntry> result;

		public Input(SearchQuery query, SearchResult<DatasetEntry> result) {
			super("search", query.toString(), M.SearchResults);
			this.query = query;
			this.result = result;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return Icon.SEARCH.descriptor();
		}
	}

}
