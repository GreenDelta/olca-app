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
import org.openlca.util.Strings;

import com.google.gson.JsonObject;

class JsonDiff {

	static boolean openDialog(DiffNode n, Direction direction, boolean editMode) {
		if (n == null)
			return false;
		var diff = n.contentAsDiffResult();
		if (diff == null)
			return false;
		var node = createNode(diff);
		var dialog = editMode
				? JsonDiffDialog.forEditing(node, direction)
				: JsonDiffDialog.forViewing(node, direction);
		dialog.setTitle(getPath(diff));
		dialog.setLogo(getLogo(diff));
		dialog.setDependencyResolver(ModelDependencyResolver.INSTANCE);
		dialog.setLabelProvider(new ModelLabelProvider());
		var code = dialog.open();
		if (code == IDialogConstants.CANCEL_ID || !editMode)
			return false;
		var localDiffersFromRemote = dialog.leftDiffersFromRight();
		var keepLocalModel = code == JsonDiffDialog.KEEP_LOCAL_MODEL;
		diff.reset();
		diff.overwriteRemoteChanges = overwriteRemoteChanges(node, localDiffersFromRemote, keepLocalModel);
		diff.overwriteLocalChanges = !diff.overwriteRemoteChanges;
		diff.mergedData = getMergedData(node, localDiffersFromRemote, keepLocalModel);
		return true;
	}

	private static JsonNode createNode(DiffResult diff) {
		var left = diff.local != null ? diff.local.right : diff.remote.left;
		var right = diff.remote != null ? diff.remote.right : diff.local.left;
		var leftJson = left != null ? RefJson.get(left) : null;
		var rightJson = right != null ? RefJson.get(right) : null;
		return new ModelNodeBuilder().build(leftJson, rightJson);
	}

	private static String getPath(DiffResult diff) {
		return diff.ref().fullPath;
	}

	private static Image getLogo(DiffResult diff) {
		var ref = diff.ref();
		if (Strings.nullOrEmpty(ref.refId))
			return Images.getForCategory(ref.type);
		return Images.get(ref.type);
	}

	private static boolean overwriteRemoteChanges(JsonNode node, boolean localDiffersFromRemote,
			boolean keepLocalModel) {
		if (node.localElement != null && node.remoteElement != null && localDiffersFromRemote)
			return true;
		return keepLocalModel;
	}

	private static JsonObject getMergedData(JsonNode node, boolean localDiffersFromRemote, boolean keepLocalModel) {
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
