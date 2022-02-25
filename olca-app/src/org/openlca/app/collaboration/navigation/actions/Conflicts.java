package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openlca.app.collaboration.dialogs.FetchDialog;
import org.openlca.app.collaboration.util.ConflictResolutionMap;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.DiffResult;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.git.GitConfig;
import org.openlca.git.model.Diff;
import org.openlca.git.util.Constants;
import org.openlca.git.util.DiffEntries;

class Conflicts {

	static ConflictResolutionMap solve() throws IOException {
		var commits = Repository.get().commits;
		var localCommitId = commits.resolve(Constants.LOCAL_BRANCH);
		var remoteCommitId = commits.resolve(Constants.REMOTE_BRANCH);
		var localChanges = getLocalChanges(localCommitId);
		var remoteChanges = getRemoteChanges(localCommitId, remoteCommitId);
		var conflicts = new ArrayList<DiffResult>();
		new ArrayList<>(localChanges).forEach(local -> {
			var remote = remoteChanges.stream()
					.filter(r -> r.ref().type == local.ref().type && r.ref().refId.equals(local.ref().refId))
					.findFirst()
					.orElse(null);
			if (remote != null) {
				conflicts.add(new DiffResult(local, remote));
				localChanges.remove(local);
			}
		});
		remoteChanges.forEach(remote -> {
			var local = localChanges.stream()
					.filter(l -> l.ref().type == remote.ref().type && l.ref().refId.equals(remote.ref().refId))
					.findFirst()
					.orElse(null);
			if (local != null) {
				conflicts.add(new DiffResult(local, remote));
			}
		});
		if (conflicts.isEmpty())
			return new ConflictResolutionMap();
		var node = new DiffNodeBuilder(Database.get()).build(conflicts);
		var dialog = new FetchDialog(node);
		if (dialog.open() == FetchDialog.CANCEL)
			return null;
		return dialog.getResolvedConflicts();
	}

	private static List<Diff> getLocalChanges(String localCommitId) throws IOException {
		var repo = Repository.get();
		var commit = repo.commits.get(localCommitId);
		var config = new GitConfig(Database.get(), repo.workspaceIds, repo.git, null);
		return DiffEntries.workspace(config, commit).stream()
				.map(d -> new Diff(d))
				.collect(Collectors.toList());
	}

	private static List<Diff> getRemoteChanges(String localCommitId, String remoteCommitId) throws IOException {
		var repo = Repository.get();
		var localCommit = repo.commits.get(localCommitId);
		var remoteCommit = repo.commits.get(remoteCommitId);
		return DiffEntries.between(repo.git, localCommit, remoteCommit).stream()
				.map(d -> new Diff(d))
				.collect(Collectors.toList());
	}

}
