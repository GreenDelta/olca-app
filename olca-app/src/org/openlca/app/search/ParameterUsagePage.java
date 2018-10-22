package org.openlca.app.search;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.trees.Trees;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.usage.ParameterUsageTree;
import org.openlca.core.database.usage.ParameterUsageTree.Node;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

public class ParameterUsagePage extends SimpleFormEditor {

	private ParameterUsageTree tree;

	public static void show(String param) {
		AtomicReference<ParameterUsageTree> ref = new AtomicReference<>();
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
			Input pin = (Input) input;
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
			ScrolledForm form = UI.formHeader(mform,
					M.UsageOf + " " + tree.param);
			FormToolkit tk = mform.getToolkit();
			Composite body = UI.formBody(form, tk);
			TreeViewer tree = Trees.createViewer(body,
					M.Context, M.UsageType, M.Formula);
			Trees.bindColumnWidths(tree.getTree(), 0.4, 0.3, 0.3);
			tree.setContentProvider(new ContentProvider());
			tree.setLabelProvider(new Label());
			tree.setInput(this.tree);
			if (this.tree.nodes.size() >= 1) {
				tree.setExpandedElements(
						new Object[] { this.tree.nodes.get(0) });
			}
			Trees.onDoubleClick(tree, e -> onOpen(tree));
			Action open = Actions.create(M.Open,
					Icon.FOLDER_OPEN.descriptor(),
					() -> onOpen(tree));
			Actions.bind(tree, open);
		}

		private void onOpen(TreeViewer tree) {
			Node node = Viewers.getFirstSelected(tree);
			if (node == null)
				return;
			node = node.root();
			if (!(node.context instanceof CategorizedDescriptor))
				return;
			App.openEditor((CategorizedDescriptor) node.context);
		}

	}

	private class ContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object old, Object newInput) {
		}

		@Override
		public Object[] getElements(Object elem) {
			if (!(elem instanceof ParameterUsageTree))
				return new Object[0];
			ParameterUsageTree tree = (ParameterUsageTree) elem;
			return tree.nodes.toArray();
		}

		@Override
		public Object[] getChildren(Object elem) {
			if (!(elem instanceof Node))
				return null;
			Node node = (Node) elem;
			if (node.childs.isEmpty())
				return null;
			return node.childs.toArray();
		}

		@Override
		public Object getParent(Object elem) {
			return null;
		}

		@Override
		public boolean hasChildren(Object elem) {
			if (!(elem instanceof Node))
				return false;
			Node node = (Node) elem;
			return !node.childs.isEmpty();
		}
	}

	private class Label extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Node))
				return null;
			Node n = (Node) obj;
			if (col == 0)
				return Images.get(n.context);
			if (col == 1 && n.type != null)
				return Icon.LINK.get();
			if (col == 2 && n.formula != null)
				return Icon.EXPRESSION.get();
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Node))
				return null;
			Node n = (Node) obj;
			switch (col) {
			case 0:
				return Labels.getDisplayName(n.context);
			case 1:
				return n.type;
			case 2:
				return n.formula;
			default:
				return null;
			}
		}
	}
}
