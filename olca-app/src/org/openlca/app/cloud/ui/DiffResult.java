package org.openlca.app.cloud.ui;

import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.cloud.model.data.DatasetDescriptor;
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

	public DatasetDescriptor getDescriptor() {
		if (local != null)
			return local.getDescriptor();
		return toDescriptor(remote);
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

	public DiffResponse getType() {
		// both null is not an option
		if (remote == null && local == null)
			return DiffResponse.NONE;
		if (remote == null)
			if (local.type == DiffType.NEW)
				return DiffResponse.ADD_TO_REMOTE;
			else if (local.type == DiffType.DELETED)
				return DiffResponse.DELETE_FROM_REMOTE;
			else if (local.type == DiffType.CHANGED)
				return DiffResponse.MODIFY_IN_REMOTE;
			else
				return DiffResponse.NONE;
		if (local == null || local.type == null)
			if (remote.isDeleted())
				return DiffResponse.NONE;
			else
				return DiffResponse.ADD_TO_LOCAL;
		// remote & local can not be null anymore
		if (local.type == DiffType.NO_DIFF)
			if (remote.isDeleted())
				return DiffResponse.DELETE_FROM_LOCAL;
			else
				return DiffResponse.MODIFY_IN_LOCAL;
		if (local.type == DiffType.DELETED && remote.isDeleted())
			return DiffResponse.NONE;
		if (checkConflict())
			return DiffResponse.CONFLICT;
		return DiffResponse.NONE;
	}

	private boolean checkConflict() {
		if (local.type == DiffType.DELETED && !remote.isDeleted())
			return true;
		if (local.type != DiffType.DELETED && remote.isDeleted())
			return true;
		if (remote.getType() != local.changed.getType())
			return true;
		if (!remote.getRefId().equals(local.changed.getRefId()))
			return true;
		if (!remote.getVersion().equals(local.changed.getVersion()))
			return true;
		if (remote.getLastChange() != local.changed.getLastChange())
			return true;
		return false;
	}

	public String getDisplayName() {
		if (local != null)
			return local.getDescriptor().getName();
		if (remote != null)
			return remote.getName();
		return null;
	}

	@Override
	public String toString() {
		String l = "null";
		if (local != null) {
			l = "type: " + local.getDescriptor().getType();
			l += ", name: " + local.getDescriptor().getName();
		}
		String text = "model: {" + l + "}, diff: {" + local.type
				+ "}, result: {" + getType() + "}";
		return text;
	}

	private DatasetDescriptor toDescriptor(FetchRequestData data) {
		DatasetDescriptor descriptor = new DatasetDescriptor();
		descriptor.setCategoryType(data.getCategoryType());
		descriptor.setCategoryRefId(data.getCategoryRefId());
		descriptor.setFullPath(data.getFullPath());
		descriptor.setType(data.getType());
		descriptor.setRefId(data.getRefId());
		descriptor.setName(data.getName());
		descriptor.setLastChange(data.getLastChange());
		descriptor.setVersion(data.getVersion());
		return descriptor;
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
