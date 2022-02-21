package org.openlca.app.collaboration.viewers.diff;

import java.time.Instant;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.collaboration.dialogs.JsonDiffDialog;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.collaboration.viewers.json.label.Direction;
import org.openlca.app.collaboration.viewers.json.olca.ModelDependencyResolver;
import org.openlca.app.collaboration.viewers.json.olca.ModelLabelProvider;
import org.openlca.app.collaboration.viewers.json.olca.ModelNodeBuilder;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.Version;
import org.openlca.git.model.Reference;
import org.openlca.util.Strings;

import com.google.gson.JsonObject;

class DiffHelper {

	static MergeResult openDiffDialog(Reference left, Reference right, boolean viewMode, Direction direction) {
		if (left == null && right == null)
			return null;
		var node = createNode(left, right, viewMode);
		var dialog = viewMode
				? JsonDiffDialog.forViewing(node, direction)
				: JsonDiffDialog.forEditing(node, direction);
		dialog.setTitle(getPath(left, right));
		dialog.setLogo(getLogo(left, right));
		dialog.setDependencyResolver(ModelDependencyResolver.INSTANCE);
		dialog.setLabelProvider(new ModelLabelProvider());
		var code = dialog.open();
		if (code == IDialogConstants.CANCEL_ID || viewMode)
			return null;
		var localDiffersFromRemote = dialog.leftDiffersFromRight();
		var keepLocalModel = code == JsonDiffDialog.KEEP_LOCAL_MODEL;
		return new MergeResult(node, localDiffersFromRemote, keepLocalModel);
	}

	private static JsonNode createNode(Reference left, Reference right, boolean viewMode) {
		var leftJson = left != null ? RefJson.get(left) : null;
		var rightJson = right != null ? RefJson.get(right) : null;
		return new ModelNodeBuilder().build(leftJson, rightJson);
	}

	private static String getPath(Reference left, Reference right) {
		var ref = left != null ? left : right;
		return ref.fullPath;
	}

	private static Image getLogo(Reference left, Reference right) {
		var ref = left != null ? left : right;
		if (Strings.nullOrEmpty(ref.refId))
			return Images.getForCategory(ref.type);
		return Images.get(ref.type);
	}

	static class MergeResult {

		final boolean overwriteLocalChanges;
		final boolean overwriteRemoteChanges;
		final JsonObject merged;

		private MergeResult(JsonNode node, boolean localDiffersFromRemote, boolean keepLocalModel) {
			overwriteRemoteChanges = overwriteRemoteChanges(node, localDiffersFromRemote, keepLocalModel);
			overwriteLocalChanges = !overwriteRemoteChanges;
			merged = getMergedData(node, localDiffersFromRemote, keepLocalModel);
		}

		private boolean overwriteRemoteChanges(JsonNode node, boolean localDiffersFromRemote,
				boolean keepLocalModel) {
			if (node.localElement != null && node.remoteElement != null && localDiffersFromRemote)
				return true;
			return keepLocalModel;
		}

		private JsonObject getMergedData(JsonNode node, boolean localDiffersFromRemote, boolean keepLocalModel) {
			JsonObject obj = null;
			if (node.localElement != null && node.remoteElement != null) {
				obj = node.localElement.getAsJsonObject();
			} else if (node.localElement != null && keepLocalModel) {
				obj = node.localElement.getAsJsonObject();
			} else if (node.remoteElement != null && !keepLocalModel) {
				obj = node.remoteElement.getAsJsonObject();
			}
			if (obj == null)
				return null;
			if (node.remoteElement == null)
				return obj;
			if (localDiffersFromRemote) {
				Version version = Version.fromString(node.remoteElement.getAsJsonObject().get("version").getAsString());
				version.incUpdate();
				obj.addProperty("version", Version.asString(version.getValue()));
			}
			obj.addProperty("lastChange", Instant.now().toString());
			return obj;
		}

	}

}
