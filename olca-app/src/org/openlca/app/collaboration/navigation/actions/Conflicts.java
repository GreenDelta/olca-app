package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.openlca.app.collaboration.dialogs.MergeDialog;
import org.openlca.app.collaboration.util.InMemoryConflictResolver;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Question;
import org.openlca.git.actions.ConflictResolver.ConflictResolution;
import org.openlca.git.actions.GitStashCreate;
import org.openlca.git.actions.GitStashDrop;
import org.openlca.git.model.Commit;
import org.openlca.git.model.Diff;
import org.openlca.git.util.Constants;
import org.openlca.git.util.Diffs;
import org.openlca.git.util.TypeRefIdMap;

class Conflicts {

	static InMemoryConflictResolver resolve(String ref, PersonIdent person, boolean stashCommit)
			throws IOException, GitAPIException, InvocationTargetException, InterruptedException {
		var repo = Repository.get();
		var commit = repo.commits.find().refs(ref).latest();
		var commonParent = repo.history.commonParentOf(Constants.LOCAL_REF, ref);
		var workspaceConflicts = Conflicts.withWorkspace(commit, commonParent);
		if (workspaceConflicts.isEmpty())
			return resolve(commit, Conflicts.withLocalHistory(commit, commonParent));
		var answers = new ArrayList<>(Arrays.asList("Cancel", "Discard changes", "Commit changes"));
		if (!stashCommit) {
			answers.add("Stash changes");
		}
		var result = Question.ask("Handle conflicts",
				"There are conflicts with uncommited changes, how do you want to proceed?",
				answers.toArray(new String[answers.size()]));
		if (result == 0)
			return null;
		if (result == 1 && !stashChanges(person, true))
			return null;
		if (result == 2)
			return commitChanges(ref, person, stashCommit);
		if (result == 3 && !stashChanges(person, false))
			return null;
		return resolve(commit, Conflicts.withLocalHistory(commit, commonParent));
	}

	private static List<TriDiff> withWorkspace(Commit commit, Commit commonParent) throws IOException {
		var repo = Repository.get();
		var workspaceChanges = Diffs.workspace(repo.toConfig());
		if (workspaceChanges.isEmpty())
			return new ArrayList<>();
		var remoteChanges = Diffs.between(repo.git, commonParent, commit);
		return between(workspaceChanges, remoteChanges);
	}

	private static List<TriDiff> withLocalHistory(Commit commit, Commit commonParent) throws IOException {
		var repo = Repository.get();
		var localCommit = repo.commits.get(repo.commits.resolve(Constants.LOCAL_BRANCH));
		if (localCommit == null)
			return new ArrayList<>();
		var localChanges = Diffs.between(repo.git, commonParent, localCommit);
		if (localChanges.isEmpty())
			return new ArrayList<>();
		var remoteChanges = Diffs.between(repo.git, commonParent, commit);
		if (remoteChanges.isEmpty())
			return new ArrayList<>();
		return between(localChanges, remoteChanges);
	}

	private static List<TriDiff> between(List<Diff> localChanges, List<Diff> remoteChanges) {
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

	private static InMemoryConflictResolver commitChanges(String ref, PersonIdent person, boolean stashCommit)
			throws InvocationTargetException, IOException, GitAPIException, InterruptedException {
		var commitAction = new CommitAction();
		commitAction.accept(Arrays.asList(Navigator.findElement(Database.getActiveConfiguration())));
		if (!commitAction.doRun(false))
			return null;
		return resolve(ref, person, stashCommit);
	}

	private static boolean stashChanges(PersonIdent person, boolean discard)
			throws GitAPIException, IOException, InvocationTargetException, InterruptedException {
		var repo = Repository.get();
		if (!discard && Actions.getStashCommit(repo.git) != null) {
			var answers = Arrays.asList("Cancel", "Discard existing stash");
			var result = Question.ask("Stash workspace",
					"You already have stashed changes, how do you want to proceed?",
					answers.toArray(new String[answers.size()]));
			if (result == 0)
				return false;
			GitStashDrop.from(repo.git).run();
		}
		var stashCreate = GitStashCreate.from(Database.get())
				.to(repo.git)
				.as(person)
				.update(repo.workspaceIds);
		if (discard) {
			stashCreate = stashCreate.discard();
		}
		Actions.run(stashCreate);
		return true;
	}

	private static InMemoryConflictResolver resolve(Commit commit, List<TriDiff> conflicts) {
		var solved = solve(conflicts);
		if (solved == null)
			return null;
		return new InMemoryConflictResolver(commit, solved);
	}

	private static TypeRefIdMap<ConflictResolution> solve(List<TriDiff> conflicts) {
		if (conflicts.isEmpty())
			return new TypeRefIdMap<>();
		var node = new DiffNodeBuilder(Database.get()).build(conflicts);
		if (node == null)
			return new TypeRefIdMap<>();
		var dialog = new MergeDialog(node);
		if (dialog.open() == MergeDialog.CANCEL)
			return null;
		return dialog.getResolvedConflicts();
	}

}
