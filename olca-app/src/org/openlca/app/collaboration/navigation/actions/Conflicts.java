package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openlca.app.collaboration.dialogs.FetchDialog;
import org.openlca.app.collaboration.util.InMemoryConflictResolver;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.git.actions.ConflictResolver.ConflictResolution;
import org.openlca.git.model.Commit;
import org.openlca.git.model.Diff;
import org.openlca.git.util.Constants;
import org.openlca.git.util.Diffs;
import org.openlca.git.util.TypeRefIdMap;

class Conflicts {

	static InMemoryConflictResolver identifyAndSolve(Commit remoteCommit) throws IOException {
		var commits = Repository.get().commits;
		var localCommit = commits.get(commits.resolve(Constants.LOCAL_BRANCH));
		var conflicts = identify(localCommit, remoteCommit);
		var solved = solve(conflicts);
		return new InMemoryConflictResolver(remoteCommit, solved);
	}

	static List<TriDiff> identify(Commit localCommit, Commit remoteCommit) throws IOException {
		if (localCommit == null)
			return new ArrayList<>();
		var repo = Repository.get();
		var head = repo.commits.head();
		if (head != null && localCommit.id.equals(head.id))
			return new ArrayList<>();
		var localChanges = Diffs.between(repo.git, head, localCommit);
		var remoteChanges = Diffs.between(repo.git, head, remoteCommit);
		return conflictsOf(localChanges, remoteChanges);
	}

	static TypeRefIdMap<ConflictResolution> solve(List<TriDiff> changes) {
		var node = new DiffNodeBuilder(Database.get()).build(changes);
		if (node == null)
			return new TypeRefIdMap<>();
		var dialog = new FetchDialog(node);
		if (dialog.open() == FetchDialog.CANCEL)
			return new TypeRefIdMap<>();
		return dialog.getResolvedConflicts();
	}

	private static List<TriDiff> conflictsOf(List<Diff> localChanges, List<Diff> remoteChanges) {
		var conflicts = new ArrayList<TriDiff>();
		new ArrayList<>(localChanges).forEach(local -> {
			var remote = remoteChanges.stream()
					.filter(r -> r.type == local.type && r.refId.equals(local.refId))
					.findFirst()
					.orElse(null);
			if (remote != null) {
				conflicts.add(new TriDiff(local, remote));
				localChanges.remove(local);
			}
		});
		remoteChanges.forEach(remote -> {
			var local = localChanges.stream()
					.filter(l -> l.type == remote.type && l.refId.equals(remote.refId))
					.findFirst()
					.orElse(null);
			if (local != null) {
				conflicts.add(new TriDiff(local, remote));
			}
		});
		return conflicts;
	}

}
