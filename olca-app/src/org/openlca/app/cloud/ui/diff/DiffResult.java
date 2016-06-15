package org.openlca.app.cloud.ui.diff;

import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Direction;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.model.data.FetchRequestData;
import org.openlca.core.model.Version;

import com.google.gson.JsonObject;

public class DiffResult {

	public final FetchRequestData remote;
	public final Diff local;
	private JsonObject mergedData;
	private boolean overwriteLocalChanges;
	private boolean overwriteRemoteChanges;
	public boolean ignoreRemote;

	public DiffResult(FetchRequestData remote) {
		this(remote, null);
	}

	public DiffResult(Diff local) {
		this(null, local);
	}

	public DiffResult(FetchRequestData remote, Diff local) {
		this.remote = remote;
		if (local != null)
			this.local = local.copy();
		else
			this.local = null;
	}

	public Dataset getDataset() {
		if (remote != null)
			return toDataset(remote);
		return local.getDataset();
	}

	private Dataset toDataset(FetchRequestData data) {
		Dataset dataset = new Dataset();
		dataset.categoryType = data.categoryType;
		dataset.categoryRefId = data.categoryRefId;
		dataset.fullPath = data.fullPath;
		dataset.type = data.type;
		dataset.refId = data.refId;
		dataset.name = data.name;
		dataset.lastChange = data.lastChange;
		dataset.version = data.version;
		return dataset;
	}

	public DiffResponse getType() {
		// both null is not an option
		if (remote == null && local == null)
			return DiffResponse.NONE;
		if (remote == null)
			if (local.type == DiffType.DELETED || local.type == DiffType.NO_DIFF)
				return DiffResponse.NONE;
			else if (!ignoreRemote)
				return DiffResponse.ADD_TO_REMOTE;
			// ignore remote means that remote was not even fetched, we have to
			// take this into account
			else if (local.type == DiffType.NEW)
				return DiffResponse.ADD_TO_REMOTE;
			else
				return DiffResponse.MODIFY_IN_REMOTE;
		if (local == null || local.type == null)
			if (remote.isDeleted())
				return DiffResponse.NONE;
			else
				return DiffResponse.ADD_TO_LOCAL;
		// remote & local can not be null anymore
		return getTypeMixed();
	}

	private DiffResponse getTypeMixed() {
		if (isEqual())
			return DiffResponse.NONE;
		switch (local.type) {
		case NO_DIFF:
			if (remote.isDeleted())
				return DiffResponse.DELETE_FROM_LOCAL;
			// TODO check if this always correct
			if (isNewer(local.getDataset().version, remote.version))
				return DiffResponse.MODIFY_IN_REMOTE;
			return DiffResponse.MODIFY_IN_LOCAL;
		case DELETED:
			if (remote.isDeleted())
				return DiffResponse.NONE;
			if (!remote.version.equals(local.dataset.version))
				return DiffResponse.CONFLICT;
			return DiffResponse.DELETE_FROM_REMOTE;
		case CHANGED:
			if (!remote.version.equals(local.dataset.version))
				return DiffResponse.CONFLICT;
			// TODO check if this always correct
			if (isNewer(local.getDataset().version, remote.version))
				return DiffResponse.MODIFY_IN_REMOTE;
			return DiffResponse.MODIFY_IN_LOCAL;
		default:
			return DiffResponse.NONE;
		}
	}

	private boolean isNewer(String version1, String version2) {
		Version local = Version.fromString(version1);
		Version remote = Version.fromString(version2);
		if (local.getMajor() > remote.getMajor())
			return true;
		if (local.getMajor() < remote.getMajor())
			return false;
		if (local.getMinor() > remote.getMinor())
			return true;
		if (local.getMinor() < remote.getMinor())
			return false;
		if (local.getUpdate() > remote.getUpdate())
			return true;
		return false;
	}

	private boolean isEqual() {
		boolean localDeleted = local.type == DiffType.DELETED;
		if (localDeleted && !remote.isDeleted())
			return false;
		if (remote.type != local.getDataset().type)
			return false;
		if (!remote.refId.equals(local.getDataset().refId))
			return false;
		if (!remote.version.equals(local.getDataset().version))
			return false;
		if (remote.lastChange != local.getDataset().lastChange)
			return false;
		return true;
	}

	boolean isConflict() {
		return getType() == DiffResponse.CONFLICT;
	}

	public String getDisplayName() {
		if (local != null)
			return local.getDataset().name;
		if (remote != null)
			return remote.name;
		return null;
	}

	public JsonObject getMergedData() {
		return mergedData;
	}

	public boolean overwriteLocalChanges() {
		return overwriteLocalChanges;
	}

	public boolean overwriteRemoteChanges() {
		return overwriteRemoteChanges;
	}

	void setMergedData(JsonObject mergedData) {
		this.mergedData = mergedData;
	}

	void setOverwriteLocalChanges(boolean overwriteLocalChanges) {
		this.overwriteLocalChanges = overwriteLocalChanges;
	}

	void setOverwriteRemoteChanges(boolean overwriteRemoteChanges) {
		this.overwriteRemoteChanges = overwriteRemoteChanges;
	}

	void reset() {
		overwriteLocalChanges = false;
		overwriteRemoteChanges = false;
		mergedData = null;
	}

	@Override
	public String toString() {
		String l = "null";
		if (local != null) {
			l = "type: " + local.getDataset().type;
			l += ", name: " + local.getDataset().name;
		}
		String text = "model: {" + l + "}, diff: {" + local.type
				+ "}, result: {" + getType() + "}";
		return text;
	}

	public static enum DiffResponse {

		NONE(Direction.LEFT_TO_RIGHT), // avoid null pointer
		ADD_TO_LOCAL(Direction.RIGHT_TO_LEFT),
		DELETE_FROM_LOCAL(Direction.RIGHT_TO_LEFT),
		MODIFY_IN_REMOTE(Direction.LEFT_TO_RIGHT),
		MODIFY_IN_LOCAL(Direction.RIGHT_TO_LEFT),
		ADD_TO_REMOTE(Direction.LEFT_TO_RIGHT),
		DELETE_FROM_REMOTE(Direction.LEFT_TO_RIGHT),
		CONFLICT(Direction.RIGHT_TO_LEFT);

		// LEFT_TO_RIGHT means merging is not possible
		// RIGHT_TO_LEFT means merging is possible
		public Direction direction;

		private DiffResponse(Direction direction) {
			this.direction = direction;
		}

		public boolean isOneOf(DiffResponse... types) {
			if (types == null)
				return false;
			for (DiffResponse type : types)
				if (type == this)
					return true;
			return false;
		}

	}

}
