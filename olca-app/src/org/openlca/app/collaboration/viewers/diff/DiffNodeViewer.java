package org.openlca.app.collaboration.viewers.diff;

import java.util.Collection;
import java.util.Map;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.dialogs.JsonCompareDialog;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.collaboration.viewers.json.olca.ModelNodeBuilder;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.ModelTypeOrder;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Labels;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.core.database.Daos;
import org.openlca.core.model.ModelType;
import org.openlca.git.actions.ConflictResolver.ConflictResolution;
import org.openlca.git.actions.ConflictResolver.ConflictResolutionType;
import org.openlca.git.model.DiffType;
import org.openlca.git.model.Reference;
import org.openlca.git.model.TriDiff;
import org.openlca.git.util.ModelRefMap;
import org.openlca.util.TypedRefIdMap;

import com.google.gson.JsonObject;

abstract class DiffNodeViewer extends AbstractViewer<DiffNode, TreeViewer> {

	DiffNode root;
	private final boolean canMerge;
	private Map<String, Reference> entryMap;
	private Runnable onMerge;
	private ModelRefMap<ConflictResolution> resolvedConflicts = new ModelRefMap<>();

	DiffNodeViewer(Composite parent, boolean canMerge) {
		super(parent);
		this.canMerge = canMerge;
		this.entryMap = Repository.CURRENT != null
				? Repository.CURRENT.references.find().includeCategories().includeLibraries().asMap()
				: null;
	}

	@Override
	public void setInput(Collection<DiffNode> collection) {
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

	public void setOnMerge(Runnable onMerge) {
		this.onMerge = onMerge;
	}

	public TypedRefIdMap<ConflictResolution> getResolvedConflicts() {
		return resolvedConflicts;
	}

	protected void onDoubleClick(DoubleClickEvent event) {
		var selected = getSelected(event);
		if (selected == null || selected.isCategoryNode())
			return;
		var diff = selected.contentAsTriDiff();
		var node = createNode(diff);
		var dialog = canMerge
				? JsonCompareDialog.forMerging(node)
				: JsonCompareDialog.forComparison(node);
		var dialogResult = dialog.open();
		if (canMerge && dialogResult != JsonCompareDialog.CANCEL) {
			var resolution = toResolution(node, dialogResult);
			resolvedConflicts.put(diff, resolution);
			if (onMerge != null) {
				onMerge.run();
			}
			getViewer().refresh(selected);
		}
	}

	private ConflictResolution toResolution(JsonNode node, int dialogResult) {
		if (dialogResult == JsonCompareDialog.OVERWRITE || node.hasEqualValues())
			return ConflictResolution.overwrite();
		if (dialogResult == JsonCompareDialog.KEEP || node.leftEqualsOriginal())
			return ConflictResolution.keep();
		var merged = RefJson.getMergedData(node);
		return ConflictResolution.merge(merged);
	}

	private JsonNode createNode(TriDiff diff) {
		if (diff == null)
			return null;
		var leftJson = getLeft(diff);
		var rightJson = getRight(diff);
		return new ModelNodeBuilder().build(leftJson, rightJson);
	}

	private JsonObject getLeft(TriDiff diff) {
		if (diff.right == null) {
			if (diff.left == null || diff.left.diffType == DiffType.ADDED)
				return null;
			return RefJson.get(diff.left.oldRef, diff);
		}
		if (diff.left == null || diff.left.diffType == DiffType.DELETED)
			return null;
		return RefJson.get(diff.left.newRef, diff);
	}

	private JsonObject getRight(TriDiff diff) {
		if (diff.right == null) {
			if (diff.left == null || diff.left.diffType == DiffType.DELETED)
				return null;
			return RefJson.get(diff.left.newRef, diff);
		}
		if (diff.right.diffType == DiffType.DELETED)
			return null;
		return RefJson.get(diff.right.newRef, diff);
	}

	private DiffNode getSelected(DoubleClickEvent event) {
		if (event.getSelection().isEmpty())
			return null;
		if (!(event.getSelection() instanceof IStructuredSelection))
			return null;
		var selection = (IStructuredSelection) event.getSelection();
		if (selection.size() > 1)
			return null;
		var selected = (DiffNode) selection.getFirstElement();
		if (selected.isModelTypeNode())
			return null;
		return selected;
	}

	protected class DiffNodeContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			var elements = (Object[]) inputElement;
			if (elements == null || elements.length == 0)
				return new Object[0];
			return getChildren(elements[0]);
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			var node = (DiffNode) parentElement;
			return node.children.toArray();
		}

		@Override
		public Object getParent(Object element) {
			var node = (DiffNode) element;
			return node.parent;
		}

		@Override
		public boolean hasChildren(Object element) {
			var node = (DiffNode) element;
			return !node.children.isEmpty();
		}

		@Override
		public void dispose() {

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}

	}

	protected class DiffNodeLabelProvider extends org.eclipse.jface.viewers.LabelProvider {

		@Override
		public String getText(Object element) {
			if (element == null)
				return null;
			var node = (DiffNode) element;
			if (node.isDatabaseNode())
				return node.contentAsDatabase().getName();
			if (node.isLibrariesNode())
				return M.Libraries;
			if (node.isLibraryNode()) {
				var path = node.contentAsTriDiff().path;
				return path.substring(path.indexOf("/") + 1);
			}
			if (node.isModelTypeNode())
				return Labels.plural(node.getModelType());
			if (node.isCategoryNode() && node.content instanceof String)
				return node.contentAsString().substring(node.contentAsString().lastIndexOf("/") + 1);
			if (node.isCategoryNode() && node.content instanceof TriDiff)
				return node.contentAsTriDiff().path.substring(node.contentAsTriDiff().path.lastIndexOf("/") + 1);
			var diff = (TriDiff) node.content;
			var descriptor = Daos.root(Database.get(), diff.type).getDescriptorForRefId(diff.refId);
			if (descriptor != null)
				return descriptor.name;
			var repo = Repository.CURRENT;
			if (diff.right != null && hasObjectId(diff.right.newRef))
				return repo.datasets.getName(diff.right.newRef);
			if (diff.left != null && hasObjectId(diff.left.newRef))
				return repo.datasets.getName(diff.left.newRef);
			if (diff.right != null && hasObjectId(diff.right.oldRef))
				return repo.datasets.getName(diff.right.oldRef);
			if (diff.left != null && hasObjectId(diff.left.oldRef))
				return repo.datasets.getName(diff.left.oldRef);
			return "";
		}

		private static boolean hasObjectId(Reference ref) {
			return ref != null && !ObjectId.zeroId().equals(ref.objectId);
		}

		@Override
		public Image getImage(Object element) {
			if (element == null)
				return null;
			var node = (DiffNode) element;
			if (node.isModelTypeNode())
				return Images.getForCategory(node.getModelType());
			if (node.isLibrariesNode())
				return Icon.FOLDER.get();
			if (node.isCategoryNode()) {
				if (node.content instanceof String s)
					if (!entryMap.containsKey(s))
						return Images.getForCategory(node.getModelType(), Overlay.ADD_TO_REMOTE);
				if (node.content instanceof TriDiff diff)
					return Images.getForCategory(node.getModelType(), getOverlay(diff));
				return Images.getForCategory(node.getModelType());
			}
			var diff = node.contentAsTriDiff();
			var overlay = getOverlay(diff);
			if (node.isLibraryNode())
				return Images.library(overlay);
			return Images.get(diff.type, overlay);
		}

		private Overlay getOverlay(TriDiff diff) {
			if (diff.noAction())
				return null;
			if (resolvedConflicts.contains(diff))
				return getOverlayMerged(diff);
			var leftDiffType = diff.left != null ? diff.left.diffType : null;
			var rightDiffType = diff.right != null ? diff.right.diffType : null;
			return getOverlay(leftDiffType, rightDiffType);
		}

		private Overlay getOverlay(DiffType prev, DiffType next) {
			if (prev == null && next == null)
				return null;
			if (prev == null)
				return getOverlayLocal(next);
			if (next == null)
				return getOverlayRemote(prev);
			return Overlay.CONFLICT;
		}

		private Overlay getOverlayLocal(DiffType type) {
			return switch (type) {
				case ADDED -> Overlay.ADD_TO_LOCAL;
				case MODIFIED, MOVED -> Overlay.MODIFY_IN_LOCAL;
				case DELETED -> Overlay.DELETE_FROM_LOCAL;
			};
		}

		private Overlay getOverlayRemote(DiffType type) {
			return switch (type) {
				case ADDED -> Overlay.ADD_TO_REMOTE;
				case MODIFIED, MOVED -> Overlay.MODIFY_IN_REMOTE;
				case DELETED -> Overlay.DELETE_FROM_REMOTE;
			};
		}

		private Overlay getOverlayMerged(TriDiff result) {
			var resolution = resolvedConflicts.get(result);
			if (resolution != null && resolution.type != ConflictResolutionType.OVERWRITE)
				return Overlay.MERGED;
			if (result.right != null && result.right.diffType == DiffType.DELETED)
				return Overlay.DELETE_FROM_LOCAL;
			if (result.left == null || result.left.diffType == DiffType.DELETED)
				return Overlay.ADD_TO_LOCAL;
			return Overlay.MODIFY_IN_LOCAL;
		}

	}

	protected class DiffNodeComparator extends ViewerComparator {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			var node1 = (DiffNode) e1;
			var node2 = (DiffNode) e2;
			return compare(viewer, node1, node2);
		}

		private int compare(Viewer viewer, DiffNode node1, DiffNode node2) {
			if (node1.isLibrariesNode())
				return 1;
			if (node2.isLibrariesNode())
				return -1;
			if (node1.isModelTypeNode() && node2.isModelTypeNode())
				return compareModelTypes(node1, node2);
			return super.compare(viewer, node1, node2);
		}

		private int compareModelTypes(DiffNode node1, DiffNode node2) {
			var type1 = (ModelType) node1.content;
			var type2 = (ModelType) node2.content;
			return ModelTypeOrder.compare(type1, type2);
		}

	}

}
