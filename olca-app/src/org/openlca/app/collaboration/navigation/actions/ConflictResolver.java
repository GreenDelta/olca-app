package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.MergeDialog;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Question;
import org.openlca.core.model.Version;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.git.actions.ConflictResolver.ConflictResolution;
import org.openlca.git.actions.GitReset;
import org.openlca.git.actions.GitStashCreate;
import org.openlca.git.actions.GitStashDrop;
import org.openlca.git.model.Commit;
import org.openlca.git.model.Diff;
import org.openlca.git.util.Constants;
import org.openlca.git.util.TypedRefIdMap;
import org.openlca.jsonld.Json;
import org.openlca.util.Strings;

import com.google.common.base.Predicates;

class ConflictResolver {

	private final Repository repo;
	private final boolean stashing;
	private final Commit remoteCommit;
	private final Commit commonParent;
	private List<Diff> remoteChanges = null;
	private Conflicts workspaceConflicts = Conflicts.none();
	private Conflicts localConflicts = Conflicts.none();

	private ConflictResolver(String ref, boolean stashing) {
		this.repo = Repository.CURRENT;
		this.stashing = stashing;
		this.remoteCommit = repo.commits.find().refs(ref).latest();
		this.commonParent = repo.localHistory.commonParentOf(ref);
	}

	static ConflictResult forRemote()
			throws InvocationTargetException, IOException, GitAPIException, InterruptedException {
		return new ConflictResolver(Constants.REMOTE_REF, false).resolve();
	}

	static ConflictResult forStash()
			throws InvocationTargetException, IOException, GitAPIException, InterruptedException {
		return new ConflictResolver(org.eclipse.jgit.lib.Constants.R_STASH, true).resolve();
	}

	private ConflictResult resolve()
			throws IOException, GitAPIException, InvocationTargetException, InterruptedException {
		initWorkspaceConflicts();
		var wasStashed = false;
		if (!workspaceConflicts.remaining.isEmpty()) {
			var answers = new ArrayList<>(Arrays.asList(M.Cancel, M.DiscardChanges, M.CommitChanges));
			if (!stashing) {
				answers.add(M.StashChanges);
			}
			var result = Question.ask(M.HandleConflicts, M.HandleConflictsQuestion,
					answers.toArray(new String[answers.size()]));
			switch (result) {
				case 0:
					return null;
				case 1:
					Actions.run(GitReset.on(repo)
							.to(repo.commits.head())
							.changes(toChanges(workspaceConflicts.remaining)));
					break;
				case 2:
					return commitChanges();
				case 3:
					if (!stashChanges(toChanges(workspaceConflicts.remaining)))
						return null;
					wasStashed = true;
					break;
			}
		}
		initLocalConflicts();
		var solved = solve();
		return new ConflictResult(new ConflictResolutionMap(solved), wasStashed);
	}

	private void initWorkspaceConflicts() {
		App.runWithProgress(M.CheckingForWorkspaceConflicts, () -> {
			var repo = Repository.CURRENT;
			var workspaceChanges = repo.diffs.find().excludeLibraries().withDatabase();
			if (workspaceChanges.isEmpty())
				return;
			remoteChanges = repo.diffs.find().excludeLibraries().commit(commonParent).with(remoteCommit);
			var diffs = between(workspaceChanges, remoteChanges);
			workspaceConflicts = splitEqualAndRemaining(diffs);
		});
	}

	private List<Diff> toChanges(List<TriDiff> remaining) {
		return remaining.stream()
				.map(diff -> diff.left)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private ConflictResult commitChanges()
			throws InvocationTargetException, IOException, GitAPIException, InterruptedException {
		var commitAction = new CommitAction();
		commitAction.accept(Arrays.asList(Navigator.findElement(Database.getActiveConfiguration())));
		if (!commitAction.doRun(false))
			return null;
		return resolve();
	}

	private boolean stashChanges(List<Diff> changes)
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

	private TypedRefIdMap<ConflictResolution> solve() {
		var solved = new TypedRefIdMap<ConflictResolution>();
		workspaceConflicts.equal.forEach(c -> solved.put(c, ConflictResolution.isEqual()));
		localConflicts.equal.forEach(c -> solved.put(c, ConflictResolution.isEqual()));
		if (localConflicts.remaining.isEmpty())
			return solved;
		localConflicts.remaining.stream()
				.filter(Predicate.not(TriDiff::conflict))
				.forEach(conflict -> solved.put(conflict, ConflictResolution.keep()));
		var node = new DiffNodeBuilder(Database.get()).build(localConflicts.remaining);
		if (node == null || node.children.isEmpty())
			return solved;
		var dialog = new MergeDialog(node);
		if (dialog.open() == MergeDialog.CANCEL)
			return null;
		dialog.getResolvedConflicts().forEach(
				(type, refId, resolution) -> solved.put(type, refId, resolution));
		return solved;
	}

	private void initLocalConflicts() throws IOException {
		var localCommit = repo.commits.get(repo.commits.resolve(Constants.LOCAL_BRANCH));
		if (localCommit == null || commonParent == null)
			return;
		if (localCommit.id.equals(commonParent.id))
			return;
		App.runWithProgress(M.CheckingForLocalConflicts, () -> {
			var localChanges = repo.diffs.find().excludeLibraries().commit(commonParent).with(localCommit);
			if (remoteChanges == null) {
				remoteChanges = repo.diffs.find().excludeLibraries().commit(commonParent).with(remoteCommit);
			}
			if (localChanges.isEmpty() || remoteChanges.isEmpty())
				return;
			var diffs = between(localChanges, remoteChanges);
			localConflicts = splitEqualAndRemaining(diffs);
		});
	}

	private List<TriDiff> between(List<Diff> localChanges, List<Diff> remoteChanges) {
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

	private TriDiff findConflict(Diff element, List<Diff> others) {
		return others.stream()
				.filter(e -> e.path.equals(element.path)
						|| (e.type == element.type && Strings.nullOrEqual(e.refId, element.refId)))
				.findFirst()
				.map(e -> new TriDiff(element, e))
				.orElse(null);
	}

	private Conflicts splitEqualAndRemaining(List<TriDiff> conflicts) {
		var equal = new ArrayList<TriDiff>();
		var remaining = new ArrayList<TriDiff>();
		conflicts.forEach(conflict -> {
			if (conflict.isCategory)
				return;
			if (equalsDescriptor(conflict, repo.descriptors.get(conflict))) {
				equal.add(conflict);
			} else {
				remaining.add(conflict);
			}
		});
		return new Conflicts(equal, remaining);
	}

	private boolean equalsDescriptor(TriDiff diff, RootDescriptor d) {
		if (d == null)
			return false;
		if (diff.right == null || diff.right.newRef == null || ObjectId.zeroId().equals(diff.right.newRef.objectId))
			return false;
		var remoteModel = repo.datasets.parse(diff.right.newRef, "lastChange", "version");
		if (remoteModel == null)
			return false;
		var version = Version.fromString(string(remoteModel, "version")).getValue();
		var lastChange = date(remoteModel, "lastChange");
		var category = repo.descriptors.categoryPaths.pathOf(d.category);
		if (category == null) {
			category = "";
		}
		return version == d.version && lastChange == d.lastChange && category.equals(diff.category);
	}

	private String string(Map<String, Object> map, String field) {
		var value = map.get(field);
		if (value == null)
			return null;
		return value.toString();
	}

	private long date(Map<String, Object> map, String field) {
		var value = map.get(field);
		if (value == null)
			return 0;
		try {
			return Long.parseLong(value.toString());
		} catch (NumberFormatException e) {
			var date = Json.parseDate(value.toString());
			if (date == null)
				return 0;
			return date.getTime();
		}
	}

	record ConflictResult(ConflictResolutionMap resolutions, boolean stashedChanges) {

	}

	record Conflicts(List<TriDiff> equal, List<TriDiff> remaining) {

		private static Conflicts none() {
			return new Conflicts(new ArrayList<>(), new ArrayList<>());
		}

	}

}
