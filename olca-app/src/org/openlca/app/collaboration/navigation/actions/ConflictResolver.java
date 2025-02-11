package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.dialogs.CommitDialog;
import org.openlca.app.collaboration.dialogs.MergeDialog;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Question;
import org.openlca.git.actions.ConflictResolver.ConflictResolution;
import org.openlca.git.actions.GitCommit;
import org.openlca.git.actions.GitReset;
import org.openlca.git.actions.GitStashCreate;
import org.openlca.git.actions.GitStashDrop;
import org.openlca.git.model.Diff;
import org.openlca.git.model.TriDiff;
import org.openlca.git.repo.Conflicts;
import org.openlca.git.util.Constants;
import org.openlca.util.TypedRefIdMap;

class ConflictResolver {

	static ConflictResult resolve(String ref)
			throws IOException, GitAPIException, InvocationTargetException, InterruptedException {
		if (ref == null)
			return null;
		var repo = Repository.CURRENT;
		var conflicts = Conflicts.of(repo, ref);
		var workspaceConflicts = App.exec(M.CheckingForWorkspaceConflicts,
				() -> SplitConflicts.of(conflicts.withWorkspace()));
		var solution = solveWorkspaceConflicts(ref, workspaceConflicts.remaining);
		if (solution == ConflictSolution.CANCELED)
			return null;
		var localConflicts = App.exec(M.CheckingForLocalConflicts,
				() -> SplitConflicts.of(conflicts.withLocal()));
		var resolved = solveLocalConflicts(localConflicts);
		workspaceConflicts.equal.forEach(c -> resolved.put(c, ConflictResolution.isEqual()));
		return new ConflictResult(new ConflictResolutionMap(resolved), solution);
	}

	private static ConflictSolution solveWorkspaceConflicts(String ref, List<TriDiff> remaining)
			throws IOException, GitAPIException, InvocationTargetException, InterruptedException {
		if (remaining.isEmpty())
			return ConflictSolution.NONE;
		var repo = Repository.CURRENT;
		while (!remaining.isEmpty()) {
			var answers = new ArrayList<>(Arrays.asList(M.Cancel, M.DiscardChanges, M.CommitChanges));
			if (!ref.equals(Constants.STASH_REF)) {
				answers.add(M.StashChanges);
			}
			var result = Question.ask(M.HandleConflicts, M.HandleConflictsQuestion,
					answers.toArray(new String[answers.size()]));
			switch (result) {
				case 1:
					Actions.run(GitReset.on(repo)
							.to(repo.commits.head())
							.changes(toChanges(remaining)));
					return ConflictSolution.DISCARDED;
				case 2:
					remaining = commitChanges(ref, remaining);
					if (remaining == null)
						return ConflictSolution.CANCELED;
					break;
				case 3:
					if (!stashChanges(toChanges(remaining)))
						return null;
					return ConflictSolution.STASHED;
				case 0:
				default:
					return ConflictSolution.CANCELED;
			}
		}
		return ConflictSolution.COMMITTED;
	}

	private static List<Diff> toChanges(List<TriDiff> remaining) {
		return remaining.stream()
				.map(diff -> diff.left)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private static List<TriDiff> commitChanges(String ref, List<TriDiff> remaining)
			throws InvocationTargetException, IOException, GitAPIException, InterruptedException {
		var repo = Repository.CURRENT;
		var diffs = remaining.stream()
				.map(c -> c.right)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		var input = Datasets.select(
				Arrays.asList(Navigator.findElement(Database.getActiveConfiguration())),
				diffs, false, false);
		if (input == null || input.action() == CommitDialog.CANCEL)
			return null;
		var user = repo.promptUser();
		if (user == null)
			return null;
		var changes = input.datasets();
		var commitId = Actions.runWithCancel(GitCommit.on(repo)
				.changes(changes)
				.withMessage(input.message())
				.as(user));
		if (commitId == null)
			return null;
		var paths = changes.stream()
				.map(c -> c.path)
				.collect(Collectors.toSet());
		return remaining.stream()
				.filter(c -> !paths.contains(c.path))
				.collect(Collectors.toList());
	}

	private static boolean stashChanges(List<Diff> changes)
			throws GitAPIException, IOException, InvocationTargetException, InterruptedException {
		var repo = Repository.CURRENT;
		var user = repo.promptUser();
		if (user == null)
			return false;
		if (repo.commits.stash() != null) {
			var answers = Arrays.asList(M.Cancel, M.DiscardExistingStash);
			var result = Question.ask(M.StashWorkspace, M.StashWorkspaceQuestion,
					answers.toArray(new String[answers.size()]));
			if (result == 0)
				return false;
			GitStashDrop.from(repo).run();
		}
		Actions.run(GitStashCreate.on(repo).as(user).changes(changes));
		return true;
	}

	private static TypedRefIdMap<ConflictResolution> solveLocalConflicts(SplitConflicts conflicts) {
		var solved = new TypedRefIdMap<ConflictResolution>();
		conflicts.equal.forEach(c -> solved.put(c, ConflictResolution.isEqual()));
		if (conflicts.remaining.isEmpty())
			return solved;
		conflicts.remaining.stream()
				.filter(Predicate.not(TriDiff::conflict))
				.forEach(c -> solved.put(c, ConflictResolution.keep()));
		var node = new DiffNodeBuilder(Database.get()).build(conflicts.remaining);
		if (node == null || node.children.isEmpty())
			return solved;
		var dialog = new MergeDialog(node);
		if (dialog.open() == MergeDialog.CANCEL)
			return null;
		solved.putAll(dialog.getResolvedConflicts());
		return solved;
	}

	record ConflictResult(ConflictResolutionMap resolutions, ConflictSolution solution) {

	}

	enum ConflictSolution {

		NONE, COMMITTED, STASHED, DISCARDED, CANCELED;

	}

	private record SplitConflicts(List<TriDiff> equal, List<TriDiff> remaining) {

		private static SplitConflicts of(List<TriDiff> conflicts) {
			var repo = Repository.CURRENT;
			var equal = new ArrayList<TriDiff>();
			var remaining = new ArrayList<TriDiff>();
			conflicts.forEach(c -> {
				if (c.right != null && repo.equalsWorkspace(c.right.newRef)) {
					equal.add(c);
				} else if (!c.isCategory) {
					remaining.add(c);
				}
			});
			return new SplitConflicts(equal, remaining);
		}

	}

}
