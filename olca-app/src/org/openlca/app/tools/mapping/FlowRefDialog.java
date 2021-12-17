package org.openlca.app.tools.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Selections;
import org.openlca.core.model.ModelType;
import org.openlca.io.maps.FlowRef;
import org.openlca.util.Strings;

class FlowRefDialog extends FormDialog {

	static void open(
			IProvider provider, Consumer<Optional<FlowRef>> fn) {
		if (provider == null || fn == null)
			return;
		AtomicReference<Tree> treeRef = new AtomicReference<>();
		App.runWithProgress("Collect flows and build tree ...", () -> {
			Tree tree = Tree.build(provider.getFlowRefs());
			treeRef.set(tree);
		}, () -> {
			Tree tree = treeRef.get();
			FlowRefDialog dialog = new FlowRefDialog(tree);
			if (dialog.open() == OK) {
				FlowRef selected = dialog.selected;
				if (selected != null) {
					selected = selected.copy();
				}
				fn.accept(Optional.ofNullable(selected));
			} else {
				fn.accept(Optional.empty());
			}
		});
	}

	private FlowRef selected;
	private final Tree tree;

	private FlowRefDialog(Tree tree) {
		super(UI.shell());
		this.tree = tree;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var tk = mform.getToolkit();
		var body = UI.formBody(mform.getForm(), tk);
		UI.gridLayout(body, 1, 10, 10);

		var filterComp = tk.createComposite(body);
		UI.gridLayout(filterComp, 2, 10, 0);
		UI.gridData(filterComp, true, false);
		var filterLabel = UI.formLabel(filterComp, tk, M.Filter);
		filterLabel.setFont(UI.boldFont());
		var filterText = UI.formText(filterComp, SWT.SEARCH);
		UI.gridData(filterText, true, false);

		var viewer = new TreeViewer(body,
				SWT.BORDER | SWT.SINGLE | SWT.VIRTUAL);
		UI.gridData(viewer.getControl(), true, true);
		viewer.setContentProvider(tree);
		viewer.setLabelProvider(tree);
		viewer.setFilters(new Filter(filterText, viewer));
		viewer.setInput(tree);

		viewer.addSelectionChangedListener(e -> {
			Object obj = Selections.firstOf(e);
			selected = obj instanceof FlowRef
					? (FlowRef) obj
					: null;
			Button ok = getButton(IDialogConstants.OK_ID);
			ok.setEnabled(selected != null);
		});

		// handle double clicks
		viewer.addDoubleClickListener(e -> {
			IStructuredSelection s = viewer.getStructuredSelection();
			if (s == null || s.isEmpty())
				return;
			Object elem = s.getFirstElement();

			// double click on a flow
			if (elem instanceof FlowRef) {
				selected = (FlowRef) elem;
				okPressed();
				return;
			}

			// double click on a category
			if (!tree.hasChildren(elem))
				return;
			selected = null;
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			if (viewer.getExpandedState(elem)) {
				viewer.collapseToLevel(elem,
						AbstractTreeViewer.ALL_LEVELS);
			} else {
				viewer.expandToLevel(elem, 1);
			}
		});
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}

	@Override
	protected Point getInitialSize() {
		int width = 800;
		int height = 800;
		Rectangle shellBounds = getShell().getDisplay().getBounds();
		int shellWidth = shellBounds.x;
		int shellHeight = shellBounds.y;
		if (shellWidth > 0 && shellWidth < width)
			width = shellWidth;
		if (shellHeight > 0 && shellHeight < height)
			height = shellHeight;
		return new Point(width, height);
	}

	private static class Tree extends ColumnLabelProvider
			implements ITreeContentProvider {

		private final Node root = new Node();

		static Tree build(List<FlowRef> refs) {
			Tree tree = new Tree();
			for (FlowRef ref : refs) {
				Node node = tree.getNode(ref.flowCategory);
				node.refs.add(ref);
			}
			tree.root.sort();
			return tree;
		}

		private Node getNode(String path) {
			if (Strings.nullOrEmpty(path))
				return root;
			String[] segs = path.split("/");
			Node parent = root;
			for (String seg : segs) {
				String s = seg.trim();
				if (Strings.nullOrEmpty(s))
					continue;
				Node node = parent.childs.stream()
						.filter(n -> s.equalsIgnoreCase(n.category))
						.findFirst().orElse(null);
				if (node == null) {
					node = new Node();
					node.category = s;
					parent.childs.add(node);
				}
				parent = node;
			}
			return parent;
		}

		@Override
		public Object[] getElements(Object obj) {
			if (!(obj instanceof Tree))
				return null;
			Tree tree = (Tree) obj;
			return getChildren(tree.root);
		}

		@Override
		public Object[] getChildren(Object obj) {
			if (!(obj instanceof Node))
				return null;
			Node n = (Node) obj;
			return Stream.concat(
					n.childs.stream(), n.refs.stream()).toArray();
		}

		@Override
		public Object getParent(Object obj) {
			return null;
		}

		@Override
		public boolean hasChildren(Object obj) {
			if (!(obj instanceof Node))
				return false;
			Node node = (Node) obj;
			return !node.childs.isEmpty()
					|| !node.refs.isEmpty();
		}

		@Override
		public String getText(Object obj) {
			if (obj instanceof Node)
				return ((Node) obj).category;
			if (obj instanceof FlowRef)
				return label((FlowRef) obj);
			return "?";
		}

		@Override
		public Image getImage(Object obj) {
			if (obj instanceof Node)
				return Images.getForCategory(ModelType.FLOW);
			if (!(obj instanceof FlowRef))
				return null;
			FlowRef ref = (FlowRef) obj;
			if (ref.flow == null || ref.flow.flowType == null)
				return Images.get(ModelType.FLOW);
			return Images.get(ref.flow.flowType);
		}
	}

	private static class Node {
		String category;
		final List<Node> childs = new ArrayList<>();
		final List<FlowRef> refs = new ArrayList<>();

		void sort() {
			childs.forEach(Node::sort);
			childs.sort((n1, n2) -> Strings.compare(n1.category, n2.category));
			refs.sort((r1, r2) -> Strings.compare(label(r1), label(r2)));
		}
	}

	private static String label(FlowRef ref) {
		if (ref == null || ref.flow == null)
			return "- none -";
		String s = ref.flow.name != null
				? ref.flow.name
				: ref.flow.refId;
		if (s == null) {
			s = "?";
		}
		if (ref.flowLocation != null) {
			s += " - " + ref.flowLocation;
		}
		return s;
	}

	private static class Filter extends ViewerFilter {

		private String term = null;

		public Filter(Text text, TreeViewer viewer) {
			text.addModifyListener(e -> {
				term = text.getText().trim().toLowerCase();
				viewer.refresh();
				expand(viewer);
			});
		}

		private void expand(TreeViewer viewer) {
			TreeItem[] items = viewer.getTree().getItems();
			while (items != null && items.length > 0) {
				TreeItem next = items[0];
				next.setExpanded(true);
				for (int i = 1; i < items.length; i++)
					items[i].setExpanded(false);
				items = next.getItems();
				viewer.refresh();
			}
		}

		@Override
		public boolean select(Viewer viewer, Object parent, Object obj) {
			if (Strings.nullOrEmpty(term))
				return true;
			return matches(obj);
		}

		private boolean matches(Object obj) {
			if (obj instanceof FlowRef)
				return label((FlowRef) obj)
						.toLowerCase().contains(term);
			if (!(obj instanceof Node))
				return false;
			Node node = (Node) obj;
			for (FlowRef ref : node.refs) {
				if (matches(ref))
					return true;
			}
			for (Node child : node.childs) {
				if (matches(child))
					return true;
			}
			return false;
		}
	}
}
