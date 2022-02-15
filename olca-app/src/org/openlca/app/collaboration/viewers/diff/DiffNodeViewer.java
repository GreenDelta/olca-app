package org.openlca.app.collaboration.viewers.diff;

import java.util.Collection;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.collaboration.model.ActionType;
import org.openlca.app.collaboration.util.RefLabels;
import org.openlca.app.navigation.ModelTypeOrder;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Labels;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.core.model.ModelType;
import org.openlca.git.model.Diff;
import org.openlca.git.model.DiffType;
import org.openlca.git.model.Reference;

abstract class DiffNodeViewer extends AbstractViewer<DiffNode, TreeViewer> {

	DiffNode root;
	private DiffHelper diffHelper;
	private ActionType action;

	DiffNodeViewer(Composite parent, ActionType action) {
		super(parent);
		this.diffHelper = new DiffHelper(action);
		this.action = action;
		setLabelProvider(action);
	}

	protected void configureViewer(TreeViewer viewer, boolean checkable) {
		viewer.setContentProvider(new DiffNodeContentProvider());
		viewer.setComparator(new DiffNodeComparator());
		viewer.addDoubleClickListener(this::onDoubleClick);
	}

	void setLabelProvider(ActionType action) {
		getViewer().setLabelProvider(new DiffNodeLabelProvider());
	}

	@Override
	public void setInput(Collection<DiffNode> collection) {
		diffHelper.reset();
		if (collection.isEmpty()) {
			root = null;
			super.setInput((Collection<DiffNode>) null);
		} else {
			root = collection.iterator().next();
			super.setInput(collection);
		}
	}

	@Override
	public void setInput(DiffNode[] input) {
		root = input[0];
		super.setInput(input);
	}

	private void onDoubleClick(DoubleClickEvent event) {
		DiffNode selected = getSelected(event);
		boolean isComparison = action == ActionType.COMPARE_AHEAD || action == ActionType.COMPARE_BEHIND;
		boolean merged = diffHelper.openDiffDialog(selected, isComparison);
		if (merged && !isComparison) {
			getViewer().refresh(selected);
			onMerge(selected);
		}
	}

	protected void onMerge(DiffNode node) {
		// subclasses may override
	}

	private DiffNode getSelected(DoubleClickEvent event) {
		if (event.getSelection().isEmpty())
			return null;
		if (!(event.getSelection() instanceof IStructuredSelection))
			return null;
		IStructuredSelection selection = (IStructuredSelection) event
				.getSelection();
		if (selection.size() > 1)
			return null;
		DiffNode selected = (DiffNode) selection.getFirstElement();
		if (selected.isModelTypeNode())
			return null;
		return selected;
	}

	private class DiffNodeContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			Object[] elements = (Object[]) inputElement;
			if (elements == null || elements.length == 0)
				return new Object[0];
			DiffNode node = (DiffNode) (elements)[0];
			return node.children.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			DiffNode node = (DiffNode) parentElement;
			return node.children.toArray();
		}

		@Override
		public Object getParent(Object element) {
			DiffNode node = (DiffNode) element;
			return node.parent;
		}

		@Override
		public boolean hasChildren(Object element) {
			DiffNode node = (DiffNode) element;
			return !node.children.isEmpty();
		}

		@Override
		public void dispose() {

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}

	}

	private class DiffNodeLabelProvider extends org.eclipse.jface.viewers.LabelProvider {

		@Override
		public String getText(Object element) {
			if (element == null)
				return null;
			DiffNode node = (DiffNode) element;
			if (node.isDatabaseNode())
				return node.contentAsDatabase().getName();
			if (node.isModelTypeNode())
				return Labels.plural(node.getModelType());
			if (node.isCategoryNode())
				return node.contentAsString().substring(node.contentAsString().lastIndexOf("/") + 1);
			DiffResult result = (DiffResult) node.content;
			return getText(result);
		}

		private String getText(DiffResult result) {
			if (result.remote != null && (action == ActionType.FETCH || action == ActionType.COMPARE_AHEAD))
				return RefLabels.getName(result.remote.ref());
			if (result.local != null)
				return RefLabels.getName(result.local.ref());
			return RefLabels.getName(result.ref());
		}

		@Override
		public Image getImage(Object element) {
			if (element == null)
				return null;
			DiffNode node = (DiffNode) element;
			if (node.isModelTypeNode() || node.isCategoryNode())
				return Images.getForCategory(node.getModelType());
			DiffResult diff = node.contentAsDiffResult();
			Reference dataset = diff.ref();
			Overlay overlay = getOverlay(diff);
			return Images.get(dataset.type, overlay);
		}

		private Overlay getOverlay(DiffResult diff) {
			if (diff.noAction())
				return null;
			if (action == ActionType.COMMIT || action == ActionType.COMPARE_BEHIND)
				return getOverlay(diff.local, diff.remote);
			if (!diff.conflict() && !diff.merged())
				return getOverlay(diff.remote, diff.local);
			return getOverlayMerged(diff);
		}

		private Overlay getOverlay(Diff prev, Diff next) {
			if (prev == null && next == null)
				return null;
			if (prev == null)
				return getOverlayLocal(next.type);
			if (next == null)
				return getOverlayRemote(prev.type);
			return Overlay.CONFLICT;
		}

		private Overlay getOverlayLocal(DiffType type) {
			switch (type) {
			case ADDED:
				return Overlay.ADD_TO_LOCAL;
			case MODIFIED:
				return Overlay.MODIFY_IN_LOCAL;
			case DELETED:
				return Overlay.DELETE_FROM_LOCAL;
			}
			return null;
		}

		private Overlay getOverlayRemote(DiffType type) {
			switch (type) {
			case ADDED:
				return Overlay.ADD_TO_REMOTE;
			case MODIFIED:
				return Overlay.MODIFY_IN_REMOTE;
			case DELETED:
				return Overlay.DELETE_FROM_REMOTE;
			}
			return null;
		}

		private Overlay getOverlayMerged(DiffResult result) {
			if (!result.overwriteLocalChanges)
				return Overlay.MERGED;
			if (result.remote.type == DiffType.DELETED)
				return Overlay.DELETE_FROM_LOCAL;
			if (result.local == null || result.local.type == DiffType.DELETED)
				return Overlay.ADD_TO_LOCAL;
			return Overlay.MODIFY_IN_LOCAL;
		}

	}

	private class DiffNodeComparator extends ViewerComparator {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			DiffNode node1 = (DiffNode) e1;
			DiffNode node2 = (DiffNode) e2;
			return compare(viewer, node1, node2);
		}

		private int compare(Viewer viewer, DiffNode node1, DiffNode node2) {
			if (node1.isModelTypeNode() && node2.isModelTypeNode())
				return compareModelTypes(node1, node2);
			return super.compare(viewer, node1, node2);
		}

		private int compareModelTypes(DiffNode node1, DiffNode node2) {
			ModelType type1 = (ModelType) node1.content;
			ModelType type2 = (ModelType) node2.content;
			return ModelTypeOrder.compare(type1, type2);
		}

	}

}
