package org.openlca.app.collaboration.util;

import org.openlca.git.actions.ConflictResolver;
import org.openlca.git.model.Commit;
import org.openlca.git.model.Reference;
import org.openlca.git.util.TypeRefIdMap;

import com.google.gson.JsonObject;

public class InMemoryConflictResolver implements ConflictResolver {

//	private final Commit remoteCommit;
	private final TypeRefIdMap<ConflictResolution> localConflicts;
//	private final TypeRefIdMap<ConflictResolution> workspaceConflicts;

	public InMemoryConflictResolver(Commit remoteCommit,
			TypeRefIdMap<ConflictResolution> localConflicts,
			TypeRefIdMap<ConflictResolution> workspaceConflicts) {
//		this.remoteCommit = remoteCommit;
		this.localConflicts = localConflicts;
//		this.workspaceConflicts = workspaceConflicts;
	}

	@Override
	public boolean isConflict(Reference ref) {
//		if (ref.commitId.equals(remoteCommit.id))
			return localConflicts.contains(ref.type, ref.refId);
//		return workspaceConflicts.contains(ref.type, ref.refId);
	}

	@Override
	public ConflictResolution resolveConflict(Reference ref, JsonObject remote) {
//		if (ref.commitId.equals(remoteCommit.id))
			return localConflicts.get(ref.type, ref.refId);
//		return workspaceConflicts.get(ref.type, ref.refId);
	}

}
