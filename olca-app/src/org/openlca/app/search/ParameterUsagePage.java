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
import org.openlca.core.model.Parameter;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.util.Strings;

public class ParameterUsagePage extends SimpleFormEditor {

	private ParameterUsageTree tree;

	public static void show(Parameter param) {
		show(param, null);
	}

	public static void show(Parameter param, RootDescriptor owner) {
		if (param == null || Strings.nullOrEmpty(param.name))
			return;
		var ref = new AtomicReference<>();
		App.runWithProgress("Search for usage",
			() -> ref.set(ParameterUsageTree.of(param, owner, Database.get())),
			() -> {
				String resultKey = Cache.getAppCache().put(ref.get());
				Input input = new Input(param.name, resultKey);
				Editors.open(input, "ParameterUsagePage");
			});
	}

	public static void show(String param) {
		var ref = new AtomicReference<>();
		App.runWithProgress("Search for usage of '" + param + "' ...",
			() -> ref.set(ParameterUsageTree.of(param, Database.get())),
			() -> {
				String resultKey = Cache.getAppCache().put(ref.get());
				Input input = new Input(param, resultKey);
				Editors.open(input, "ParameterUsagePage");
			});
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		super.init(site, input);
		if (!(input instanceof Input pin)) {
			tree = ParameterUsageTree.empty();
		} else {
			tree = Cache.getAppCache().remove(
				pin.id, ParameterUsageTree.class);
			if (tree == null) {
				tree = ParameterUsageTree.empty();
			}
		}
	}

	@Override
	protected FormPage getPage() {
		return new Page(tree);
	}

	private static class Input extends SimpleEditorInput {

		Input(String param, String resultKey) {
			super(resultKey, M.UsageOf + " " + param);
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
