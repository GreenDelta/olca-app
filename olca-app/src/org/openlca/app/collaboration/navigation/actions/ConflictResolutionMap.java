package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.dialogs.MergeDialog;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Question;
import org.openlca.core.model.Version;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.git.actions.ConflictResolver;
import org.openlca.git.actions.GitDiscard;
import org.openlca.git.actions.GitStashCreate;
import org.openlca.git.actions.GitStashDrop;
import org.openlca.git.model.Commit;
import org.openlca.git.model.Diff;
import org.openlca.git.model.ModelRef;
import org.openlca.git.model.Reference;
import org.openlca.git.util.Constants;
import org.openlca.git.util.TypedRefIdMap;
import org.openlca.jsonld.Json;
import org.openlca.util.Strings;

import com.google.common.base.Predicates;
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
		var repo = Repository.CURRENT;
		var commit = repo.commits.find().refs(ref).latest();
		var commonParent = repo.localHistory.commonParentOf(ref);
		var workspaceConflicts = workspaceDiffs(commit, commonParent);
		if (workspaceConflicts.remaining.isEmpty()) {
			var resolved = resolve(commit, localDiffs(commit, commonParent), workspaceConflicts);
			if (resolved == null)
				return null;
			return new ConflictResult(resolved, false);
		}
		var answers = new ArrayList<>(Arrays.asList(M.Cancel, M.DiscardChanges, M.CommitChanges));
		if (!stashCommit) {
			answers.add(M.StashChanges);
		}
		var result = Question.ask(M.HandleConflicts, M.HandleConflictsQuestion,
				answers.toArray(new String[answers.size()]));
		switch (result) {
			case 0:
				return null;
			case 1:
				Actions.run(GitDiscard.on(repo));
				break;
			case 2:
				var resolved = commitChanges(ref, stashCommit);
				if (resolved == null)
					return null;
				return new ConflictResult(resolved, false);
			case 3:
				if (!stashChanges())
					return null;
				break;
		}
		var resolved = resolve(commit, localDiffs(commit, commonParent), Conflicts.none());
		if (resolved == null)
			return null;
		return new ConflictResult(resolved, result == 3);
	}

	private static Conflicts workspaceDiffs(Commit commit, Commit commonParent) throws IOException {
		var repo = Repository.CURRENT;
		var workspaceChanges = repo.diffs.find().withDatabase();
		if (workspaceChanges.isEmpty())
			return Conflicts.none();
		var remoteChanges = repo.diffs.find().commit(commonParent).with(commit);
		var diffs = between(workspaceChanges, remoteChanges);
		return check(diffs);
	}

	private static Conflicts localDiffs(Commit commit, Commit commonParent) throws IOException {
		var repo = Repository.CURRENT;
		var localCommit = repo.commits.get(repo.commits.resolve(Constants.LOCAL_BRANCH));
		if (localCommit == null)
			return Conflicts.none();
		var localChanges = repo.diffs.find().commit(commonParent).with(localCommit);
		if (localChanges.isEmpty())
			return Conflicts.none();
		var remoteChanges = repo.diffs.find().commit(commonParent).with(commit);
		if (remoteChanges.isEmpty())
			return Conflicts.none();
		var diffs = between(localChanges, remoteChanges);
		return check(diffs);
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
				.filter(e -> e.path.equals(element.path)
						|| (e.type == element.type && Strings.nullOrEqual(e.refId, element.refId)))
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

	private static boolean stashChanges()
			throws GitAPIException, IOException, InvocationTargetException, InterruptedException {
		var repo = Repository.CURRENT;
		var user = AuthenticationDialog.promptUser(repo);
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
		Actions.run(GitStashCreate.on(repo).as(user));
		return true;
	}

	private static TypedRefIdMap<ConflictResolution> solve(List<TriDiff> conflicts) {
		if (conflicts.isEmpty())
			return new TypedRefIdMap<>();
		var resolved = new TypedRefIdMap<ConflictResolution>();
		if (conflicts.isEmpty())
			return resolved;
		var node = new DiffNodeBuilder(Database.get()).build(conflicts);
		if (node == null || node.children.isEmpty())
			return resolved;
		var dialog = new MergeDialog(node);
		if (dialog.open() == MergeDialog.CANCEL)
			return null;
		dialog.getResolvedConflicts().forEach(
				(type, refId, resolution) -> resolved.put(type, refId, resolution));
		return resolved;
	}

	private static Conflicts check(List<TriDiff> conflicts) {
		var equal = new ArrayList<TriDiff>();
		var remaining = new ArrayList<TriDiff>();
		conflicts.forEach(conflict -> {
			if (conflict.isCategory)
				return;
			if (equalsDescriptor(conflict, Repository.CURRENT.descriptors.get(conflict))) {
				equal.add(conflict);
			} else {
				remaining.add(conflict);
			}
		});
		return new Conflicts(equal, remaining);
	}

	private static ConflictResolutionMap resolve(Commit commit, Conflicts local, Conflicts workspace) {
		var solved = solve(local.remaining);
		if (solved == null)
			return null;
		workspace.equal.forEach(c -> solved.put(c, ConflictResolution.isEqual()));
		local.equal.forEach(c -> solved.put(c, ConflictResolution.isEqual()));
		local.remaining.stream()
				.filter(Predicate.not(TriDiff::conflict))
				.forEach(conflict -> solved.put(conflict, ConflictResolution.keep()));
		return new ConflictResolutionMap(solved);
	}

	private static boolean equalsDescriptor(TriDiff diff, RootDescriptor d) {
		if (d == null)
			return false;
		if (ObjectId.zeroId().equals(diff.rightNewObjectId))
			return false;
		var ref = new Reference(diff.path, diff.commitId, diff.rightNewObjectId);
		var remoteModel = Repository.CURRENT.datasets.parse(ref, "lastChange", "version");
		if (remoteModel == null)
			return false;
		var version = Version.fromString(string(remoteModel, "version")).getValue();
		var lastChange = date(remoteModel, "lastChange");
		var category = Repository.CURRENT.descriptors.categoryPaths.pathOf(d.category);
		if (category == null) {
			category = "";
		}
		return version == d.version && lastChange == d.lastChange && category.equals(diff.category);
	}

	private static String string(Map<String, Object> map, String field) {
		var value = map.get(field);
		if (value == null)
			return null;
		return value.toString();
	}

	private static long date(Map<String, Object> map, String field) {
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
