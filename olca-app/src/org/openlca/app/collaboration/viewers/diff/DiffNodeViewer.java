package org.openlca.app.collaboration.viewers.diff;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.collaboration.dialogs.JsonDiffDialog;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.collaboration.viewers.json.label.Direction;
import org.openlca.app.collaboration.viewers.json.olca.ModelNodeBuilder;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.ModelTypeOrder;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Labels;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.core.database.Daos;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Version;
import org.openlca.git.actions.ConflictResolver.ConflictResolution;
import org.openlca.git.actions.ConflictResolver.ConflictResolutionType;
import org.openlca.git.model.DiffType;
import org.openlca.git.util.TypeRefIdMap;

abstract class DiffNodeViewer extends AbstractViewer<DiffNode, TreeViewer> {

	DiffNode root;
	private final boolean editMode;
	private Direction direction;
	private Runnable onMerge;
	private Map<ModelType, Map<String, String>> nameCache = new HashMap<>();
	private TypeRefIdMap<ConflictResolution> resolvedConflicts = new TypeRefIdMap<>();

	DiffNodeViewer(Composite parent, boolean editMode) {
		super(parent);
		this.editMode = editMode;
		getViewer().setLabelProvider(new DiffNodeLabelProvider());
	}

	protected void configureViewer(TreeViewer viewer, boolean checkable) {
		viewer.setContentProvider(new DiffNodeContentProvider());
		viewer.setComparator(new DiffNodeComparator());
		viewer.addDoubleClickListener(this::onDoubleClick);
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

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public void setOnMerge(Runnable onMerge) {
		this.onMerge = onMerge;
	}

	public TypeRefIdMap<ConflictResolution> getResolvedConflicts() {
		return resolvedConflicts;
	}

	private void onDoubleClick(DoubleClickEvent event) {
		var selected = getSelected(event);
		if (selected == null)
			return;
		var diff = selected.contentAsDiffResult();
		var node = createNode(diff);
		var dialogResult = JsonDiffDialog.open(node, direction, editMode);
		if (editMode && dialogResult != JsonDiffDialog.CANCEL) {
			var resolution = toResolution(node, dialogResult);
			resolvedConflicts.put(diff.type, diff.refId, resolution);
			if (onMerge != null) {
				onMerge.run();
			}
			getViewer().refresh(selected);
		}
	}

	private ConflictResolution toResolution(JsonNode node, int dialogResult) {
		if (dialogResult == JsonDiffDialog.OVERWRITE_LOCAL || node.hasEqualValues())
			return ConflictResolution.overwriteLocal();
		if (dialogResult == JsonDiffDialog.KEEP_LOCAL_MODEL || node.leftEqualsOriginal())
			return ConflictResolution.keepLocal();
		var merged = node.left.getAsJsonObject();
		var version = Version.fromString(node.right.getAsJsonObject().get("version").getAsString());
		version.incUpdate();
		merged.addProperty("version", Version.asString(version.getValue()));
		merged.addProperty("lastChange", Instant.now().toString());
		return ConflictResolution.merge(merged);
	}

	private JsonNode createNode(DiffResult diff) {
		if (diff == null)
			return null;
		var leftJson = diff.leftDiffType != null && diff.leftDiffType != DiffType.DELETED
				? RefJson.get(diff.type, diff.refId, diff.leftObjectId)
				: null;
		var rightJson = diff.rightDiffType != null && diff.rightDiffType != DiffType.DELETED
				? RefJson.get(diff.type, diff.refId, diff.rightObjectId)
				: null;
		return new ModelNodeBuilder().build(leftJson, rightJson);
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

	private class DiffNodeContentProvider implements ITreeContentProvider {

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

	private class DiffNodeLabelProvider extends org.eclipse.jface.viewers.LabelProvider {

		@Override
		public String getText(Object element) {
			if (element == null)
				return null;
			var node = (DiffNode) element;
			if (node.isDatabaseNode())
				return node.contentAsDatabase().getName();
			if (node.isModelTypeNode())
				return Labels.plural(node.getModelType());
			if (node.isCategoryNode())
				return node.contentAsString().substring(node.contentAsString().lastIndexOf("/") + 1);
			var result = (DiffResult) node.content;
			var names = nameCache.computeIfAbsent(result.type, this::getLabels);
			var name = names.get(result.refId);
			if (name != null)
				return name;
			if (result.rightObjectId != null)
				return Repository.get().datasets.getName(result.rightObjectId);
			return Repository.get().datasets.getName(result.leftObjectId);
		}

		private Map<String, String> getLabels(ModelType type) {
			var labels = new HashMap<String, String>();
			for (var descriptor : Daos.root(Database.get(), type).getDescriptors()) {
				labels.put(descriptor.refId, descriptor.name);
			}
			return labels;
		}

		@Override
		public Image getImage(Object element) {
			if (element == null)
				return null;
			var node = (DiffNode) element;
			if (node.isModelTypeNode() || node.isCategoryNode())
				return Images.getForCategory(node.getModelType());
			var diff = node.contentAsDiffResult();
			var overlay = getOverlay(diff);
			return Images.get(diff.type, overlay);
		}

		private Overlay getOverlay(DiffResult diff) {
			if (diff.noAction())
				return null;
			if (resolvedConflicts.contains(diff.type, diff.refId))
				return getOverlayMerged(diff);
			return getOverlay(diff.leftDiffType, diff.rightDiffType);
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
				case MODIFIED -> Overlay.MODIFY_IN_LOCAL;
				case DELETED -> Overlay.DELETE_FROM_LOCAL;
			};
		}

		private Overlay getOverlayRemote(DiffType type) {
			return switch (type) {
				case ADDED -> Overlay.ADD_TO_REMOTE;
				case MODIFIED -> Overlay.MODIFY_IN_REMOTE;
				case DELETED -> Overlay.DELETE_FROM_REMOTE;
			};
		}

		private Overlay getOverlayMerged(DiffResult result) {
			var resolution = resolvedConflicts.get(result.type, result.refId);
			if (resolution != null && resolution.type != ConflictResolutionType.OVERWRITE_LOCAL)
				return Overlay.MERGED;
			if (result.rightDiffType == DiffType.DELETED)
				return Overlay.DELETE_FROM_LOCAL;
			if (result.leftDiffType == null || result.leftDiffType == DiffType.DELETED)
				return Overlay.ADD_TO_LOCAL;
			return Overlay.MODIFY_IN_LOCAL;
		}

	}

	private class DiffNodeComparator extends ViewerComparator {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			var node1 = (DiffNode) e1;
			var node2 = (DiffNode) e2;
			return compare(viewer, node1, node2);
		}

		private int compare(Viewer viewer, DiffNode node1, DiffNode node2) {
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
