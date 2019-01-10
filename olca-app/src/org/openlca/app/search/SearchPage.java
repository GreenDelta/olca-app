package org.openlca.app.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Labels;
import org.openlca.core.database.usage.IUseSearch;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

public class SearchPage extends SimpleFormEditor {

	private Input input;
	private List<BaseDescriptor> results;

	public static void show(String term, List<BaseDescriptor> results) {
		String resultKey = Cache.getAppCache().put(results);
		Input input = new Input(term, resultKey);
		Editors.open(input, "SearchPage");
	}

	public static void forUsage(CategorizedDescriptor d) {
		if (d == null || d.type == null)
			return;
		String title = "Find usages of " + Labels.getDisplayName(d);
		AtomicReference<List<BaseDescriptor>> ref = new AtomicReference<>();
		App.run(title, () -> {
			List<CategorizedDescriptor> list = IUseSearch.FACTORY.createFor(
					d.type, Database.get()).findUses(d);
			if (d != null) {
				ref.set(new ArrayList<>(list));
			}
		}, () -> {
			String resultKey = Cache.getAppCache().put(ref.get());
			Input input = new Input(d, resultKey);
			Editors.open(input, "SearchPage");
		});

	}

	@Override
	@SuppressWarnings("unchecked")
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
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
		String title = input.getName() +
				" (" + results.size() + " " + M.Results + ")";
		return new ResultPage(this, title, results);
	}

	private static class Input extends SimpleEditorInput {

		final boolean forSearch;
		String resultKey;

		public Input(String term, String resultKey) {
			super("search", resultKey,
					M.SearchResults + ": " + term);
			forSearch = true;
			this.resultKey = resultKey;
		}

		public Input(CategorizedDescriptor d, String resultKey) {
			super("search", resultKey,
					M.UsageOf + " " + Labels.getDisplayName(d));
			forSearch = false;
			this.resultKey = resultKey;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			if (forSearch)
				return Icon.SEARCH.descriptor();
			else
				return Icon.LINK.descriptor();
		}
	}

}
