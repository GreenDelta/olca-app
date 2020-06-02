package org.openlca.app.cloud.ui.diff;

import java.util.ArrayList;

import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.model.data.FetchRequestData;
import org.openlca.core.model.ModelType;

import com.google.gson.JsonObject;

public class DiffResult {

	public final FetchRequestData remote;
	public final Diff local;
	public JsonObject mergedData;
	public boolean overwriteLocalChanges;
	public boolean overwriteRemoteChanges;

	public DiffResult(Diff local, FetchRequestData remote) {
		this.local = local != null ? local.copy() : null;
		this.remote = remote;
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
		dataset.categories = data.categories != null ? new ArrayList<>(data.categories) : null;
		dataset.type = data.type;
		dataset.refId = data.refId;
		dataset.name = data.name;
		dataset.lastChange = data.lastChange;
		dataset.version = data.version;
		return dataset;
	}

	public boolean noAction() {
		if (local == null && remote == null)
			return true;
		if (local == null || remote == null)
			return false;
		if (local.type == DiffType.DELETED)
			return remote.isDeleted();
		return !remote.isDeleted() && local.getDataset().equals(remote);
	}
	
	public boolean conflict() {
		if (local == null || remote == null)
			return false;
		switch (local.type) {
		case NEW:
			return local.getDataset().type != ModelType.CATEGORY || !local.dataset.equals(remote);
		case CHANGED:
			return remote.isDeleted() || !local.dataset.equals(remote);
		case DELETED: 
			return remote.isAdded() || !local.dataset.equals(remote);
		case NO_DIFF:
			return false;
		}
		return false;
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
			l = "modelType: " + local.getDataset().type;
			l += ", name: " + local.getDataset().name;
			l += ", type: " + local.type;
		}
		String r = "null";
		if (remote != null) {
			r = "modelType: " + remote.type;
			r += ", name: " + remote.name;
		}
		String text = "local: {" + l + "}, remote: {" + r + "}";
		return text;
	}

}
