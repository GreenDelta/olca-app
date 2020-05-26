package org.openlca.app.cloud.ui.diff;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.ui.compare.ModelLabelProvider;
import org.openlca.app.cloud.ui.compare.ModelNodeBuilder;
import org.openlca.app.cloud.ui.compare.ModelUtil;
import org.openlca.app.cloud.ui.compare.json.DiffEditorDialog;
import org.openlca.app.cloud.ui.compare.json.IDependencyResolver;
import org.openlca.app.cloud.ui.compare.json.JsonNode;
import org.openlca.app.cloud.ui.compare.json.JsonUtil;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.Version;

import com.google.gson.JsonObject;

class CompareHelper {

	private JsonLoader loader;
	private ActionType action;
	private Map<String, JsonNode> nodes = new HashMap<>();

	CompareHelper(JsonLoader loader, ActionType action) {
		this.loader = loader;
		this.action = action;
	}

	boolean openDiffEditor(DiffNode node, boolean viewMode) {
		if (node == null || node.isModelTypeNode())
			return false;
		DiffData data = new DiffData();
		data.result = (DiffResult) node.content;
		DiffEditorDialog dialog = prepareDialog(data, viewMode);
		dialog.setTitle(getTitle(node));
		dialog.setLogo(getLogo(node));
		int code = dialog.open();
		if (code == IDialogConstants.CANCEL_ID)
			return false;
		if (!data.result.conflict())
			return false;
		boolean localDiffersFromRemote = dialog.leftDiffersFromRight();
		boolean keepLocalModel = code == DiffEditorDialog.KEEP_LOCAL_MODEL;
		updateResult(data, localDiffersFromRemote, keepLocalModel);
		return true;
	}

	void reset() {
		nodes.clear();
	}

	private DiffEditorDialog prepareDialog(DiffData data, boolean viewMode) {
		data.node = nodes.get(data.result.getDataset().toId());
		if (data.node == null) {
			createNode(data);
		} else {
			data.local = JsonUtil.toJsonObject(data.node.localElement);
			data.remote = JsonUtil.toJsonObject(data.node.remoteElement);
		}
		IDependencyResolver depResolver = ModelUtil.getDependencyResolver();
		if (viewMode || !data.result.conflict())
			return DiffEditorDialog.forViewing(data.node, new ModelLabelProvider(), depResolver, action);
		DiffEditorDialog dialog = DiffEditorDialog.forEditing(data.node, new ModelLabelProvider(), depResolver, action);
		return dialog;
	}

	private void createNode(DiffData data) {
		if (data.result.local != null) {
			data.local = loader.getLocalJson(data.result.local.getDataset());
		}
		if (data.result.remote != null && !data.result.remote.isDeleted()) {
			data.remote = loader.getRemoteJson(data.result.remote);
		} else if (action == ActionType.COMMIT || action == ActionType.COMPARE_BEHIND) {
			data.remote = loader.getRemoteJson(data.result.getDataset());
		}
		data.node = new ModelNodeBuilder().build(data.local, data.remote);
		nodes.put(data.result.getDataset().toId(), data.node);
	}

	private void updateResult(DiffData data, boolean localDiffersFromRemote, boolean keepLocalModel) {
		data.result.reset();
		if (overwriteRemoteChanges(data, localDiffersFromRemote, keepLocalModel)) {
			data.result.overwriteRemoteChanges = true;
		} else {
			data.result.overwriteLocalChanges = true;
		}
		data.result.mergedData = getMergedData(data, localDiffersFromRemote, keepLocalModel);
	}

	private boolean overwriteRemoteChanges(DiffData data, boolean localDiffersFromRemote, boolean keepLocalModel) {
		if (data.hasLocal() && data.hasRemote() && localDiffersFromRemote)
			return true;
		return keepLocalModel;
	}

	private JsonObject getMergedData(DiffData data, boolean localDiffersFromRemote, boolean keepLocalModel) {
		JsonObject obj = null;
		if (data.hasLocal() && data.hasRemote()) {
			obj = data.local;
		} else if (data.hasLocal() && keepLocalModel) {
			obj = data.local;
		} else if (data.hasRemote() && !keepLocalModel) {
			obj = data.remote;
		}
		if (obj == null)
			return null;
		if (!data.hasRemote())
			return obj;
		if (localDiffersFromRemote) {
			Version version = Version.fromString(data.remote.get("version").getAsString());
			version.incUpdate();
			obj.addProperty("version", Version.asString(version.getValue()));
		}
		obj.addProperty("lastChange", Instant.now().toString());
		return obj;
	}

	private String getTitle(DiffNode node) {
		if (node.isModelTypeNode())
			return null;
		DiffResult result = (DiffResult) node.content;
		return CloudUtil.toFullPath(result.getDataset());
	}

	private Image getLogo(DiffNode node) {
		if (node.isModelTypeNode())
			return null;
		if (node.isModelNode())
			return Images.get(node.getModelType());
		DiffResult result = (DiffResult) node.content;
		return Images.getForCategory(result.getDataset().categoryType);
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
