package org.openlca.app.collaboration.util;

import org.openlca.git.actions.ConflictResolver;
import org.openlca.git.actions.ConflictResolver.ConflictResolution;
import org.openlca.git.model.Reference;

import com.google.gson.JsonObject;

public class ConflictResolutionMap extends TypeRefIdMap<ConflictResolution> implements ConflictResolver {

	@Override
	public boolean isConflict(Reference ref) {
		return contains(ref);
	}

	@Override
	public ConflictResolution resolveConflict(Reference ref, JsonObject remote) {
		return get(ref);
	}

}
