package org.openlca.app.cloud.ui.diff;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.ui.compare.ModelLabelProvider;
import org.openlca.app.cloud.ui.compare.ModelNodeBuilder;
import org.openlca.app.cloud.ui.compare.ModelUtil;
import org.openlca.app.cloud.ui.compare.json.DiffEditorDialog;
import org.openlca.app.cloud.ui.compare.json.JsonNode;
import org.openlca.app.cloud.ui.compare.json.JsonUtil;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Direction;
import org.openlca.app.util.Images;
import org.openlca.cloud.model.data.Dataset;

import com.google.gson.JsonObject;

class MergeHelper {

	private JsonLoader loader;
	private Direction direction;
	private Map<String, JsonNode> nodes = new HashMap<>();

	MergeHelper(JsonLoader loader, Direction direction) {
		this.loader = loader;
		this.direction = direction;
	}

	boolean openDiffEditor(DiffNode selected) {
		DiffData data = new DiffData();
		data.result = (DiffResult) selected.content;
		DiffEditorDialog dialog = prepareDialog(data);
		dialog.setTitle(getTitle(selected));
		dialog.setLogo(getLogo(selected));
		int code = dialog.open();
		if (code == IDialogConstants.CANCEL_ID)
			return false;
		if (!data.result.isConflict())
			return false;
		boolean localDiffersFromRemote = dialog.leftDiffersFromRight();
		boolean keepLocalModel = code == DiffEditorDialog.KEEP_LOCAL_MODEL;
		updateResult(data, localDiffersFromRemote, keepLocalModel);
		return true;
	}

	private DiffEditorDialog prepareDialog(DiffData data) {
		data.node = nodes.get(toKey(data.result.getDataset()));
		if (data.node == null) {
			if (data.result.local != null)
				data.local = loader
						.getLocalJson(data.result.local.getDataset());
			if (data.result.remote != null && !data.result.remote.isDeleted())
				data.remote = loader.getRemoteJson(data.result.remote);
			data.node = new ModelNodeBuilder().build(data.local, data.remote);
			nodes.put(toKey(data.result.getDataset()), data.node);
		} else {
			data.local = JsonUtil.toJsonObject(data.node.leftElement);
			data.remote = JsonUtil.toJsonObject(data.node.rightElement);
		}
		if (!data.result.isConflict())
			return DiffEditorDialog.forViewing(data.node,
					new ModelLabelProvider(),
					ModelUtil.getDependencyResolver(), direction);
		return DiffEditorDialog.forEditing(data.node, new ModelLabelProvider(),
				ModelUtil.getDependencyResolver(), direction);
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

	private String getTitle(DiffNode node) {
		if (node.isModelTypeNode())
			return null;
		DiffResult result = (DiffResult) node.content;
		return result.getDataset().getFullPath();
	}

	private Image getLogo(DiffNode node) {
		if (node.isModelTypeNode())
			return null;
		if (node.isModelNode())
			return Images.getIcon(node.getModelType());
		DiffResult result = (DiffResult) node.content;
		return Images.getCategoryIcon(result.getDataset().getCategoryType());
	}

	private String toKey(Dataset dataset) {
		return dataset.getType().name() + dataset.getRefId();
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
}
