package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.dialogs.MergeDialog;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Question;
import org.openlca.git.actions.ConflictResolver;
import org.openlca.git.actions.GitStashCreate;
import org.openlca.git.actions.GitStashDrop;
import org.openlca.git.model.Commit;
import org.openlca.git.model.Diff;
import org.openlca.git.model.ModelRef;
import org.openlca.git.util.Constants;
import org.openlca.git.util.Diffs;
import org.openlca.git.util.TypeRefIdMap;

import com.google.common.base.Predicates;
import com.google.gson.JsonObject;

class ConflictResolutionMap implements ConflictResolver {

	private final TypeRefIdMap<ConflictResolution> resolutions;

	ConflictResolutionMap(Commit remoteCommit, TypeRefIdMap<ConflictResolution> resolutions) {
		this.resolutions = resolutions;
	}

	@Override
	public boolean isConflict(ModelRef ref) {
		return resolutions.contains(ref);
	}

	@Override
	public ConflictResolution resolveConflict(ModelRef ref, JsonObject remote) {
		return resolutions.get(ref);
	}

	static ConflictResult forRemote()
			throws InvocationTargetException, IOException, GitAPIException, InterruptedException {
		return resolve(Constants.REMOTE_REF, false);
	}

	static ConflictResult forStash()
			throws InvocationTargetException, IOException, GitAPIException, InterruptedException {
		return resolve(org.eclipse.jgit.lib.Constants.R_STASH, true);
	}

	private static ConflictResult resolve(String ref, boolean stashCommit)
			throws IOException, GitAPIException, InvocationTargetException, InterruptedException {
		var repo = Repository.get();
		var commit = repo.commits.find().refs(ref).latest();
		var commonParent = repo.localHistory.commonParentOf(ref);
		var workspaceConflicts = workspaceDiffs(commit, commonParent);
		if (workspaceConflicts.isEmpty()) {
			var resolved = resolve(commit, localDiffs(commit, commonParent));
			if (resolved == null)
				return null;
			return new ConflictResult(resolved, false);
		}
		var answers = new ArrayList<>(Arrays.asList("Cancel", "Discard changes", "Commit changes"));
		if (!stashCommit) {
			answers.add("Stash changes");
		}
		var result = Question.ask("Handle conflicts",
				"There are conflicts with uncommited changes, how do you want to proceed?",
				answers.toArray(new String[answers.size()]));
		if (result == 0)
			return null;
		if (result == 1 && !stashChanges(true))
			return null;
		if (result == 2) {
			var resolved = commitChanges(ref, stashCommit);
			if (resolved == null)
				return null;
			return new ConflictResult(resolved, false);
		}
		if (result == 3 && !stashChanges(false))
			return null;
		var resolved = resolve(commit, localDiffs(commit, commonParent));
		if (resolved == null)
			return null;
		return new ConflictResult(resolved, result == 3);
	}

	private static List<TriDiff> workspaceDiffs(Commit commit, Commit commonParent) throws IOException {
		var repo = Repository.get();
		var workspaceChanges = Diffs.of(repo.git).with(Database.get(), repo.workspaceIds);
		if (workspaceChanges.isEmpty())
			return new ArrayList<>();
		var remoteChanges = Diffs.of(repo.git, commonParent).with(commit);
		return between(workspaceChanges, remoteChanges);
	}

	private static List<TriDiff> localDiffs(Commit commit, Commit commonParent) throws IOException {
		var repo = Repository.get();
		var localCommit = repo.commits.get(repo.commits.resolve(Constants.LOCAL_BRANCH));
		if (localCommit == null)
			return new ArrayList<>();
		var localChanges = Diffs.of(repo.git, commonParent).with(localCommit);
		if (localChanges.isEmpty())
			return new ArrayList<>();
		var remoteChanges = Diffs.of(repo.git, commonParent).with(commit);
		if (remoteChanges.isEmpty())
			return new ArrayList<>();
		return between(localChanges, remoteChanges);
	}

	private static List<TriDiff> between(List<Diff> localChanges, List<Diff> remoteChanges) {
		var conflicts = new ArrayList<TriDiff>();
		new ArrayList<>(localChanges).forEach(local -> {
			var conflict = findConflict(local, remoteChanges);
			if (conflict != null) {
				localChanges.remove(local);
				conflicts.add(conflict);
			}
		});
		remoteChanges.stream()
				.map(remote -> findConflict(remote, localChanges))
				.filter(Predicates.notNull())
				.forEach(conflicts::add);
		return conflicts;
	}

	private static TriDiff findConflict(Diff element, List<Diff> others) {
		return others.stream()
				.filter(e -> e.path.equals(element.path))
				.findFirst()
				.map(e -> new TriDiff(element, e))
				.orElse(null);
	}

	private static ConflictResolutionMap commitChanges(String ref, boolean stashCommit)
			throws InvocationTargetException, IOException, GitAPIException, InterruptedException {
		var commitAction = new CommitAction();
		commitAction.accept(Arrays.asList(Navigator.findElement(Database.getActiveConfiguration())));
		if (!commitAction.doRun(false))
			return null;
		return resolve(ref, stashCommit).resolutions;
	}

	private static boolean stashChanges(boolean discard)
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
				.update(repo.workspaceIds);
		if (discard) {
			stashCreate = stashCreate.discard();
		} else {
			var user = AuthenticationDialog.promptUser(repo);
			stashCreate = stashCreate.as(user);
		}
		Actions.run(stashCreate);
		return true;
	}

	private static ConflictResolutionMap resolve(Commit commit, List<TriDiff> conflicts) {
		var solved = solve(conflicts);
		if (solved == null)
			return null;
		conflicts.stream()
				.filter(Predicate.not(TriDiff::conflict))
				.forEach(conflict -> solved.put(conflict, ConflictResolution.keep()));
		return new ConflictResolutionMap(commit, solved);
	}

	private static TypeRefIdMap<ConflictResolution> solve(List<TriDiff> conflicts) {
		if (conflicts.isEmpty())
			return new TypeRefIdMap<>();
		var node = new DiffNodeBuilder(Database.get()).build(conflicts);
		if (node == null || node.children.isEmpty())
			return new TypeRefIdMap<>();
		var dialog = new MergeDialog(node);
		if (dialog.open() == MergeDialog.CANCEL)
			return null;
		return dialog.getResolvedConflicts();
	}

	record ConflictResult(ConflictResolutionMap resolutions, boolean stashedChanges) {

	}

}
