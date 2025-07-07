package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.dialogs.ConflictDialog;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.db.Database;
import org.openlca.git.actions.ConflictResolver.ConflictResolution;
import org.openlca.git.actions.ConflictResolver.ConflictResolutionType;
import org.openlca.git.actions.ConflictResolver.GitContext;
import org.openlca.git.model.Diff;
import org.openlca.git.model.TriDiff;
import org.openlca.git.repo.Conflicts;
import org.openlca.util.TypedRefIdMap;

class ConflictResolver {

	private final Repository repo;
	private final String ref;
	private final ConflictResolutions resolutions = new ConflictResolutions();

	private ConflictResolver(Repository repo, String ref) {
		this.repo = repo;
		this.ref = ref;
	}

	static ConflictResolutions resolve(Repository repo, String ref)
			throws IOException, GitAPIException, InvocationTargetException, InterruptedException {
		if (repo == null || ref == null)
			return null;
		return new ConflictResolver(repo, ref).resolve();
	}

	private ConflictResolutions resolve()
			throws IOException, GitAPIException, InvocationTargetException, InterruptedException {
		var conflicts = App.exec(M.CheckingForConflicts, () -> Conflicts.of(repo, ref));
		if (!solve(conflicts.local, GitContext.LOCAL))
			return null;
		var updatedWorkspaceConflicts = updateWorkspaceConflictsWithLocalResolutions(conflicts);
		if (!solve(updatedWorkspaceConflicts, GitContext.WORKSPACE))
			return null;
		return resolutions;
	}

	private boolean solve(List<TriDiff> conflicts, GitContext context) {
		var remaining = conflicts.stream()
				.filter(c -> {
					if (c.equalsWorkspace(repo)) {
						resolutions.put(c, ConflictResolution.isEqual(context));
						return false;
					}
					if (!c.isConflict()) {
						resolutions.put(c, ConflictResolution.keep(context));
						return false;
					}
					return true;
				})
				.collect(Collectors.toList());
		var node = new DiffNodeBuilder(Database.get()).build(remaining);
		if (node == null || node.children.isEmpty())
			return true;
		var dialog = new ConflictDialog(repo, resolutions, context, node);
		return dialog.open() != ConflictDialog.CANCEL;
	}

	private List<TriDiff> updateWorkspaceConflictsWithLocalResolutions(Conflicts conflicts) {
		var updated = new ArrayList<TriDiff>();
		var local = new TypedRefIdMap<Diff>();
		conflicts.local.forEach(c -> local.put(c, c.left));
		conflicts.workspace.forEach(c -> {
			var resolution = resolutions.get(c, GitContext.LOCAL);
			if (resolution == null
					|| resolution.type == ConflictResolutionType.IS_EQUAL
					|| resolution.type == ConflictResolutionType.OVERWRITE) {
				updated.add(c);
				return;
			}
			updated.add(new TriDiff(c.left, local.get(c)));
		});
		return updated;
	}

}
