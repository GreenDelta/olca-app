package org.openlca.app.search;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ParameterUsageView;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.core.database.usage.ParameterUsageTree;

public class ParameterUsagePage extends SimpleFormEditor {

	private ParameterUsageTree tree;

	public static void show(String param) {
		var ref = new AtomicReference<>();
		App.runWithProgress("Search for usage of '" + param + "' ...", () -> {
			ref.set(ParameterUsageTree.build(param, Database.get()));
		}, () -> {
			String resultKey = Cache.getAppCache().put(ref.get());
			Input input = new Input(param, resultKey);
			Editors.open(input, "ParameterUsagePage");
		});
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		if (!(input instanceof Input)) {
			tree = new ParameterUsageTree("?");
		} else {
			var pin = (Input) input;
			tree = Cache.getAppCache().remove(
					pin.id, ParameterUsageTree.class);
			if (tree == null) {
				tree = new ParameterUsageTree("?");
			}
		}
	}

	@Override
	protected FormPage getPage() {
		return new Page(tree);
	}

	private static class Input extends SimpleEditorInput {

		Input(String param, String resultKey) {
			super("parameter.usage", resultKey,
					M.UsageOf + " " + param);
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return Icon.LINK.descriptor();
		}
	}

	private class Page extends FormPage {

		private final ParameterUsageTree tree;

		public Page(ParameterUsageTree tree) {
			super(ParameterUsagePage.this,
					"ParameterUsagePage",
					M.UsageOf + " " + tree.param);
			this.tree = tree;
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(
					mform, M.UsageOf + " " + tree.param);
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);
			ParameterUsageView.show(body, tree);
		}
	}
}
