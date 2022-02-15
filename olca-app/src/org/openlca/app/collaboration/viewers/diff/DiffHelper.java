package org.openlca.app.collaboration.viewers.diff;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.collaboration.dialogs.DiffDialog;
import org.openlca.app.collaboration.model.ActionType;
import org.openlca.app.collaboration.util.Json;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.collaboration.viewers.json.olca.ModelDependencyResolver;
import org.openlca.app.collaboration.viewers.json.olca.ModelLabelProvider;
import org.openlca.app.collaboration.viewers.json.olca.ModelNodeBuilder;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.Version;
import org.openlca.git.model.DiffType;

import com.google.gson.JsonObject;

class DiffHelper {

	private ActionType action;
	private Map<String, JsonNode> nodes = new HashMap<>();

	DiffHelper(ActionType action) {
		this.action = action;
	}

	boolean openDiffDialog(DiffNode node, boolean viewMode) {
		if (node == null || !node.isModelNode())
			return false;
		DiffData data = new DiffData();
		data.result = node.contentAsDiffResult();
		DiffDialog dialog = prepareDialog(data, viewMode);
		dialog.setTitle(getTitle(node));
		dialog.setLogo(getLogo(node));
		dialog.setDependencyResolver(ModelDependencyResolver.INSTANCE);
		dialog.setLabelProvider(new ModelLabelProvider());
		int code = dialog.open();
		if (code == IDialogConstants.CANCEL_ID)
			return false;
		if (viewMode || !data.result.conflict())
			return false;
		boolean localDiffersFromRemote = dialog.leftDiffersFromRight();
		boolean keepLocalModel = code == DiffDialog.KEEP_LOCAL_MODEL;
		updateResult(data, localDiffersFromRemote, keepLocalModel);
		return true;
	}

	void reset() {
		nodes.clear();
	}

	private DiffDialog prepareDialog(DiffData data, boolean viewMode) {
		data.node = nodes.get(data.result.ref().fullPath);
		if (data.node == null) {
			prepareData(data);
			if (!viewMode) {
				nodes.put(data.result.ref().fullPath, data.node);
			}
		} else {
			data.local = Json.toJsonObject(data.node.localElement);
			data.remote = Json.toJsonObject(data.node.remoteElement);
		}
		if (viewMode || !data.result.conflict())
			return DiffDialog.forViewing(data.node, action);
		return DiffDialog.forEditing(data.node, action);
	}

	private void prepareData(DiffData data) {
		if (data.result.local != null) {
			data.local = RefJson.get(data.result.local.ref());
		}
		if (data.result.remote != null && data.result.remote.type != DiffType.DELETED) {
			data.remote = RefJson.get(data.result.remote.ref());
		}
		data.node = new ModelNodeBuilder().build(data.local, data.remote);
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
		if (!node.isModelNode())
			return null;
		return node.contentAsDiffResult().ref().fullPath;
	}

	private Image getLogo(DiffNode node) {
		if (node.isModelTypeNode())
			return null;
		if (node.isModelNode())
			return Images.get(node.getModelType());
		return Images.getForCategory(node.getModelType());
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
