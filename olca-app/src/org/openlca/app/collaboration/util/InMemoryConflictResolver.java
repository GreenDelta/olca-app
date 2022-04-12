package org.openlca.app.collaboration.util;

import org.openlca.git.actions.ConflictResolver;
import org.openlca.git.model.Commit;
import org.openlca.git.model.ModelRef;
import org.openlca.git.util.TypeRefIdMap;

import com.google.gson.JsonObject;

public class InMemoryConflictResolver implements ConflictResolver {

	private final TypeRefIdMap<ConflictResolution> conflicts;

	public InMemoryConflictResolver(Commit remoteCommit, TypeRefIdMap<ConflictResolution> conflicts) {
		this.conflicts = conflicts;
	}

	@Override
	public boolean isConflict(ModelRef ref) {
		return conflicts.contains(ref.type, ref.refId);
	}

	@Override
	public ConflictResolution resolveConflict(ModelRef ref, JsonObject remote) {
		return conflicts.get(ref.type, ref.refId);
	}

}
