package org.openlca.app.cloud.ui.diff;

import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.model.data.FetchRequestData;

import com.google.gson.JsonObject;

public class DiffResult {

	public final FetchRequestData remote;
	public final Diff local;
	private JsonObject mergedData;
	private boolean overwriteLocalChanges;
	private boolean overwriteRemoteChanges;

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
			return getTypeFromLocal();
		if (local == null || local.type == null)
			if (remote.isDeleted())
				return DiffResponse.NONE;
			else
				return DiffResponse.ADD_TO_LOCAL;
		// remote & local can not be null anymore
		return getTypeMixed();
	}

	private DiffResponse getTypeFromLocal() {
		switch (local.type) {
		case NEW:
			return DiffResponse.ADD_TO_REMOTE;
		case DELETED:
			return DiffResponse.DELETE_FROM_REMOTE;
		case CHANGED:
			return DiffResponse.MODIFY_IN_REMOTE;
		default:
			return DiffResponse.NONE;
		}
	}

	private DiffResponse getTypeMixed() {
		switch (local.type) {
		case NO_DIFF:
			if (remote.isDeleted())
				return DiffResponse.DELETE_FROM_LOCAL;
			return DiffResponse.MODIFY_IN_LOCAL;
		case DELETED:
			if (remote.isDeleted())
				return DiffResponse.NONE;
		default:
			if (checkConflict())
				return DiffResponse.CONFLICT;
			return DiffResponse.NONE;
		}
	}

	private boolean checkConflict() {
		boolean localDeleted = local.type == DiffType.DELETED;
		if (localDeleted && !remote.isDeleted())
			return true;
		if (localDeleted && remote.isDeleted())
			return true;
		if (remote.type != local.changed.type)
			return true;
		if (!remote.refId.equals(local.changed.refId))
			return true;
		if (!remote.version.equals(local.changed.version))
			return true;
		if (remote.lastChange != local.changed.lastChange)
			return true;
		return false;
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

		NONE, ADD_TO_LOCAL, DELETE_FROM_LOCAL, MODIFY_IN_REMOTE, MODIFY_IN_LOCAL, ADD_TO_REMOTE, DELETE_FROM_REMOTE, CONFLICT;

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
