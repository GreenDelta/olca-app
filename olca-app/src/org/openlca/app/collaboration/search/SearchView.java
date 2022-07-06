package org.openlca.app.collaboration.search;

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

public class SearchView extends SimpleFormEditor {

	private Input input;

	public static void open(SearchQuery query) {
		Editors.open(new Input(query), "SearchView");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		this.input = (Input) input;
	}

	@Override
	protected FormPage getPage() {
		return new SearchPage(this, input.query);
	}

	private static class Input extends SimpleEditorInput {

		private final SearchQuery query;

		public Input(SearchQuery query) {
			super(query.toString(), M.SearchResults);
			this.query = query;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return Icon.SEARCH.descriptor();
		}
	}

}
