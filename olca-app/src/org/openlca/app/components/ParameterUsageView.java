package org.openlca.app.components;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.database.usage.ParameterUsageTree;
import org.openlca.core.database.usage.ParameterUsageTree.UsageType;

/**
 * Renders a parameter usage tree in a composite.
 */
public class ParameterUsageView {

	private final ParameterUsageTree tree;

	private ParameterUsageView(ParameterUsageTree tree) {
		this.tree = tree;
	}

	public static void show(Composite comp, ParameterUsageTree tree) {
		new ParameterUsageView(tree).render(comp);
	}

	private void render(Composite comp) {
		var tree = Trees.createViewer(comp,
				M.Context, M.UsageType, M.Formula);
		Trees.bindColumnWidths(tree.getTree(), 0.4, 0.3, 0.3);
		tree.setContentProvider(new ContentProvider());
		tree.setLabelProvider(new Label());
		tree.setInput(this.tree);
		if (this.tree.nodes.size() >= 1) {
			tree.setExpandedElements(this.tree.nodes.get(0));
		}
		Trees.onDoubleClick(tree, e -> onOpen(tree));
		var open = Actions.create(M.Open,
				Icon.FOLDER_OPEN.descriptor(),
				() -> onOpen(tree));
		Actions.bind(tree, open);
	}

	private void onOpen(TreeViewer tree) {
		ParameterUsageTree.Node node = Viewers.getFirstSelected(tree);
		if (node == null)
			return;
		var root = node.root();
		if (root == null || root.model == null)
			return;
		App.open(root.model);
	}

	private static class ContentProvider implements ITreeContentProvider {

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
			var tree = (ParameterUsageTree) elem;
			return tree.nodes.toArray();
		}

		@Override
		public Object[] getChildren(Object elem) {
			if (!(elem instanceof ParameterUsageTree.Node))
				return null;
			var node = (ParameterUsageTree.Node) elem;
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
			if (!(elem instanceof ParameterUsageTree.Node))
				return false;
			var node = (ParameterUsageTree.Node) elem;
			return !node.childs.isEmpty();
		}
	}

	private static class Label extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof ParameterUsageTree.Node))
				return null;
			var node = (ParameterUsageTree.Node) obj;
			if (col == 0)
				return modelIcon(node);
			if (col == 1 && node.usageType != null)
				return Icon.LINK.get();
			if (col == 2 && node.usage != null)
				return Icon.EXPRESSION.get();
			return null;
		}

		private Image modelIcon(ParameterUsageTree.Node node) {
			var n = node;
			do {
				if (n.model != null)
					return Images.get(n.model);
				n = n.parent;
			} while (n != null);
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ParameterUsageTree.Node))
				return null;
			var node = (ParameterUsageTree.Node) obj;
			switch (col) {
			case 0:
				return node.model != null
						? Labels.name(node.model)
						: node.name;
			case 1:
				return usageType(node.usageType);
			case 2:
				return node.usage;
			default:
				return null;
			}
		}

		private String usageType(UsageType type) {
			if (type == null)
				return "";
			switch (type) {
			case FORMULA:
				return M.Formula;
			case DEFINITION:
				return "Parameter definition";
			case REDEFINITION:
				return "Parameter redefinition";
			default:
				return "?";
			}
		}
	}
}
