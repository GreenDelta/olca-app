package org.openlca.app.collaboration.navigation.actions;

import org.openlca.git.actions.ConflictResolver;
import org.openlca.git.model.ModelRef;
import org.openlca.util.TypedRefIdMap;

import com.google.gson.JsonObject;

class ConflictResolutionMap implements ConflictResolver {

	private final TypedRefIdMap<ConflictResolution> resolutions;

	ConflictResolutionMap(TypedRefIdMap<ConflictResolution> resolutions) {
		this.resolutions = resolutions;
	}

	@Override
	public boolean isConflict(ModelRef ref) {
		return resolutions.contains(ref);
	}

	@Override
	public ConflictResolutionType peekConflictResolution(ModelRef ref) {
		var resolution = resolutions.get(ref);
		if (resolution == null)
			return null;
		return resolution.type;
	}

	@Override
	public ConflictResolution resolveConflict(ModelRef ref, JsonObject remote) {
		return resolutions.get(ref);
	}

}
