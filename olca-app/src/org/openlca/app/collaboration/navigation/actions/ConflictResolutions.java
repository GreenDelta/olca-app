package org.openlca.app.collaboration.navigation.actions;

import org.openlca.core.model.TypedRefId;
import org.openlca.util.TypedRefIdMap;

import com.google.gson.JsonObject;

public class ConflictResolutions implements org.openlca.git.actions.ConflictResolver {

	private TypedRefIdMap<ConflictResolution> local = new TypedRefIdMap<>();
	private TypedRefIdMap<ConflictResolution> workspace = new TypedRefIdMap<>();
	
	public boolean contains(TypedRefId ref, GitContext context) {
		if (context == GitContext.LOCAL)
			return local.contains(ref);
		if (context == GitContext.WORKSPACE)
			return workspace.contains(ref);
		return false;
	}

	public ConflictResolution get(TypedRefId ref, GitContext context) {
		if (context == GitContext.LOCAL)
			return local.get(ref);
		if (context == GitContext.WORKSPACE)
			return workspace.get(ref);
		return null;
	}

	public void put(TypedRefId ref, ConflictResolution resolution) {
		if (resolution.context == GitContext.LOCAL) {
			local.put(ref, resolution);
		} else if (resolution.context == GitContext.WORKSPACE) {
			workspace.put(ref, resolution);
		}
	}

	public void clear(GitContext context) {
		if (context == GitContext.LOCAL) {
			local.clear();
		} else if (context == GitContext.WORKSPACE) {
			workspace.clear();
		}
	}

	@Override
	public boolean isConflict(TypedRefId ref) {
		return local.contains(ref) || workspace.contains(ref);
	}

	@Override
	public ConflictResolutionInfo peekConflictResolution(TypedRefId ref) {
		return resolveConflict(ref, null);
	}

	@Override
	public ConflictResolutionInfo peekConflictResolutionWithWorkspace(TypedRefId ref) {
		return resolveConflictWithWorkspace(ref, null);
	}

	@Override
	public ConflictResolution resolveConflict(TypedRefId ref, JsonObject remote) {
		if (local.contains(ref))
			return local.get(ref);
		return workspace.get(ref);
	}

	@Override
	public ConflictResolution resolveConflictWithWorkspace(TypedRefId ref, JsonObject remote) {
		return workspace.get(ref);
	}

}