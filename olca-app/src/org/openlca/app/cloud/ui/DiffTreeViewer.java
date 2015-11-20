package org.openlca.app.cloud.ui;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.openlca.app.cloud.ui.DiffResult.DiffResponse;
import org.openlca.app.cloud.ui.compare.DiffEditorDialog;
import org.openlca.app.cloud.ui.compare.JsonNode;
import org.openlca.app.cloud.ui.compare.JsonNodeBuilder;
import org.openlca.app.cloud.ui.compare.JsonUtil;
import org.openlca.app.navigation.ModelTypeComparison;
import org.openlca.app.rcp.ImageManager;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.cloud.model.data.DatasetDescriptor;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;

import com.google.gson.JsonObject;

class DiffTreeViewer extends AbstractViewer<DiffNode, TreeViewer> {

	private Function<DiffResult, JsonObject> getLocalJson;
	private Function<DiffResult, JsonObject> getRemoteJson;
	private Map<String, JsonNode> nodes = new HashMap<>();
	private Runnable onMerge;
	private DiffNode root;

	public DiffTreeViewer(Composite parent,
			Function<DiffResult, JsonObject> getJson) {
		this(parent, getJson, getJson);
	}

	public DiffTreeViewer(Composite parent,
			Function<DiffResult, JsonObject> getLocalJson,
			Function<DiffResult, JsonObject> getRemoteJson) {
		super(parent);
		this.getLocalJson = getLocalJson;
		this.getRemoteJson = getRemoteJson;
	}

	public void setOnMerge(Runnable onMerge) {
		this.onMerge = onMerge;
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent, SWT.BORDER);
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(getLabelProvider());
		viewer.setSorter(new Sorter());
		viewer.addDoubleClickListener(this::onDoubleClick);
		Tree tree = viewer.getTree();
		UI.gridData(tree, true, true);
		return viewer;
	}

	private void onDoubleClick(DoubleClickEvent event) {
		if (event.getSelection().isEmpty())
			return;
		if (!(event.getSelection() instanceof IStructuredSelection))
			return;
		IStructuredSelection selection = (IStructuredSelection) event
				.getSelection();
		if (selection.size() > 1)
			return;
		DiffNode selected = (DiffNode) selection.getFirstElement();
		if (selected.isModelTypeNode())
			return;
		openDiffEditor(selected);
	}

	private void openDiffEditor(DiffNode selected) {
		DiffData data = new DiffData();
		data.result = (DiffResult) selected.content;
		DiffEditorDialog dialog = prepareDialog(data);
		int code = dialog.open();
		if (code == IDialogConstants.CANCEL_ID)
			return;
		if (!data.result.isConflict())
			return;
		boolean localDiffersFromRemote = dialog.localDiffersFromRemote();
		boolean keepLocalModel = code == DiffEditorDialog.KEEP_LOCAL_MODEL;
		updateResult(data, localDiffersFromRemote, keepLocalModel);
		getViewer().refresh(selected);
		if (onMerge != null)
			onMerge.run();
	}

	private DiffEditorDialog prepareDialog(DiffData data) {
		data.node = nodes.get(toKey(data.result.getDescriptor()));
		if (data.node == null) {
			data.local = getLocalJson.apply(data.result);
			data.remote = getRemoteJson.apply(data.result);
			data.node = new JsonNodeBuilder().build(data.local, data.remote);
			nodes.put(toKey(data.result.getDescriptor()), data.node);
		} else {
			data.local = JsonUtil.toJsonObject(data.node.getLocalElement());
			data.remote = JsonUtil.toJsonObject(data.node.getRemoteElement());
		}
		if (!data.result.isConflict())
			return DiffEditorDialog.forViewing(data.node);
		return DiffEditorDialog.forEditing(data.node);
	}

	private void updateResult(DiffData data, boolean localDiffersFromRemote,
			boolean keepLocalModel) {
		data.result.reset();
		if (overwriteRemoteChanges(data, localDiffersFromRemote, keepLocalModel))
			data.result.setOverwriteRemoteChanges(true);
		else
			data.result.setOverwriteLocalChanges(true);
		data.result.setMergedData(getMergedData(data, keepLocalModel));
	}

	private boolean overwriteRemoteChanges(DiffData data,
			boolean localDiffersFromRemote, boolean keepLocalModel) {
		if (data.hasLocal() && data.hasRemote() && localDiffersFromRemote)
			return true;
		return keepLocalModel;
	}

	private JsonObject getMergedData(DiffData data, boolean keepLocalModel) {
		if (data.hasLocal() && data.hasRemote())
			return data.local;
		if (data.hasLocal() && keepLocalModel)
			return data.local;
		if (data.hasRemote() && !keepLocalModel)
			return data.remote;
		return null;
	}

	@Override
	public void setInput(Collection<DiffNode> collection) {
		root = collection.iterator().next();
		super.setInput(collection);
	}

	@Override
	public void setInput(DiffNode[] input) {
		root = input[0];
		super.setInput(input);
	}

	public boolean hasConflicts() {
		Stack<DiffNode> nodes = new Stack<>();
		nodes.addAll(root.children);
		while (!nodes.isEmpty()) {
			DiffNode node = nodes.pop();
			nodes.addAll(node.children);
			if (node.isModelTypeNode())
				continue;
			DiffResult result = (DiffResult) node.content;
			if (result.getType() != DiffResponse.CONFLICT)
				continue;
			if (result.overwriteLocalChanges())
				continue;
			if (result.overwriteRemoteChanges())
				continue;
			if (result.getMergedData() != null)
				continue;
			return true;
		}
		return false;
	}

	private String toKey(DatasetDescriptor descriptor) {
		return descriptor.getType().name() + descriptor.getRefId();
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new LabelProvider();
	}

	private class DiffData {
		private JsonObject local;
		private JsonObject remote;
		private JsonNode node;
		private DiffResult result;

		private boolean hasLocal() {
			return local != null;
		}

		private boolean hasRemote() {
			return remote != null;
		}
	}

	private class ContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			DiffNode node = (DiffNode) ((Object[]) inputElement)[0];
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

	private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider {

		@Override
		public String getText(Object element) {
			if (element == null)
				return null;
			DiffNode node = (DiffNode) element;
			if (node.isModelTypeNode())
				return Labels.modelType((ModelType) node.content);
			return ((DiffResult) node.content).getDisplayName();
		}

		@Override
		public Image getImage(Object element) {
			if (element == null)
				return null;
			DiffNode node = (DiffNode) element;
			if (node.isModelTypeNode())
				return Images.getIcon(dummyCategory((ModelType) node.content));
			return getImage((DiffResult) node.content);
		}

		private Image getImage(DiffResult diff) {
			DatasetDescriptor descriptor = diff.getDescriptor();
			ImageType image = null;
			if (descriptor.getType() == ModelType.CATEGORY)
				image = Images.getImageType(dummyCategory(descriptor
						.getCategoryType()));
			else
				image = Images.getImageType(descriptor.getType());
			ImageType overlay = getOverlay(diff);
			if (overlay == null)
				return ImageManager.getImage(image);
			return ImageManager.getImageWithOverlay(image, overlay);
		}

		private ImageType getOverlay(DiffResult result) {
			DiffResponse response = result.getType();
			if (response == null)
				return null;
			switch (response) {
			case ADD_TO_LOCAL:
				return ImageType.OVERLAY_ADD_TO_LOCAL;
			case ADD_TO_REMOTE:
				return ImageType.OVERLAY_ADD_TO_REMOTE;
			case MODIFY_IN_LOCAL:
				return ImageType.OVERLAY_MODIFY_IN_LOCAL;
			case MODIFY_IN_REMOTE:
				return ImageType.OVERLAY_MODIFY_IN_REMOTE;
			case DELETE_FROM_LOCAL:
				return ImageType.OVERLAY_DELETE_FROM_LOCAL;
			case DELETE_FROM_REMOTE:
				return ImageType.OVERLAY_DELETE_FROM_REMOTE;
			case CONFLICT:
				if (result.getMergedData() == null
						&& !result.overwriteLocalChanges()
						&& !result.overwriteRemoteChanges())
					return ImageType.OVERLAY_CONFLICT;
				return ImageType.OVERLAY_MERGED;
			default:
				return null;
			}
		}

		private Category dummyCategory(ModelType type) {
			Category dummy = new Category();
			dummy.setModelType(type);
			return dummy;
		}
	}

	private class Sorter extends ViewerSorter {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			DiffNode node1 = (DiffNode) e1;
			DiffNode node2 = (DiffNode) e2;
			return compare(viewer, node1, node2);
		}

		private int compare(Viewer viewer, DiffNode node1, DiffNode node2) {
			if (node1.isModelTypeNode() && node2.isModelTypeNode())
				return compareModelTypes(node1, node2);
			if (node1.isCategoryNode() && node2.isModelNode())
				return -1;
			if (node1.isModelNode() && node2.isCategoryNode())
				return 1;
			return super.compare(viewer, node1, node2);
		}

		private int compareModelTypes(DiffNode node1, DiffNode node2) {
			ModelType type1 = (ModelType) node1.content;
			ModelType type2 = (ModelType) node2.content;
			return ModelTypeComparison.compare(type1, type2);
		}

	}

}
