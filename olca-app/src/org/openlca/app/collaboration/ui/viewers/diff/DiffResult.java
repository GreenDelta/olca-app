package org.openlca.app.collaboration.ui.viewers.diff;

import org.openlca.git.model.Diff;
import org.openlca.git.model.DiffType;
import org.openlca.git.model.Reference;

import com.google.gson.JsonObject;

public class DiffResult {

	public final Diff remote;
	public final Diff local;
	public JsonObject mergedData;
	public boolean overwriteLocalChanges;
	public boolean overwriteRemoteChanges;

	public DiffResult(Diff local, Diff remote) {
		this.local = local;
		this.remote = remote;
	}

	public Reference ref() {
		return diff().ref();
	}

	public Diff diff() {
		if (local != null)
			return local;
		return remote;
	}

	public boolean noAction() {
		if (local == null && remote == null)
			return true;
		if (local == null || remote == null)
			return false;
		if (local.type == DiffType.DELETED)
			return remote.type == DiffType.DELETED;
		return local.ref().equals(remote.ref());
	}

	public boolean conflict() {
		if (local == null || remote == null)
			return false;
		switch (local.type) {
		case ADDED, MODIFIED:
			return !local.ref().equals(remote.ref());
		case DELETED:
			return remote.type != DiffType.DELETED;
		}
		return false;
	}

	public boolean merged() {
		return mergedData != null || overwriteLocalChanges || overwriteRemoteChanges;
	}

	void reset() {
		overwriteLocalChanges = false;
		overwriteRemoteChanges = false;
		mergedData = null;
	}

}
