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
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.dialogs.MergeDialog;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Question;
import org.openlca.core.database.Daos;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Version;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.git.actions.ConflictResolver;
import org.openlca.git.actions.GitStashCreate;
import org.openlca.git.actions.GitStashDrop;
import org.openlca.git.model.Commit;
import org.openlca.git.model.Diff;
import org.openlca.git.model.ModelRef;
import org.openlca.git.model.Reference;
import org.openlca.git.util.Constants;
import org.openlca.git.util.Diffs;
import org.openlca.git.util.TypeRefIdMap;
import org.openlca.jsonld.Json;

import com.google.common.base.Predicates;
import com.google.gson.JsonObject;

class ConflictResolutionMap implements ConflictResolver {

	private final TypeRefIdMap<ConflictResolution> resolutions;

	ConflictResolutionMap(TypeRefIdMap<ConflictResolution> resolutions) {
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
		var repo = Repository.get();
		var commit = repo.commits.find().refs(ref).latest();
		var commonParent = repo.localHistory.commonParentOf(ref);
		var workspaceConflicts = workspaceDiffs(commit, commonParent);
		if (workspaceConflicts.remaining.isEmpty()) {
			var resolved = resolve(commit, localDiffs(commit, commonParent), workspaceConflicts);
			if (resolved == null)
				return null;
			return new ConflictResult(resolved, false);
		}
		var answers = new ArrayList<>(Arrays.asList("Cancel", "Discard changes", "Commit changes"));
		if (!stashCommit) {
			answers.add("Stash changes");
		}
		var result = Question.ask("Handle conflicts",
				"There are conflicts with uncommitted changes, how do you want to proceed?",
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
		var resolved = resolve(commit, localDiffs(commit, commonParent), Conflicts.none());
		if (resolved == null)
			return null;
		return new ConflictResult(resolved, result == 3);
	}

	private static Conflicts workspaceDiffs(Commit commit, Commit commonParent) throws IOException {
		var repo = Repository.get();
		var workspaceChanges = Diffs.of(repo.git).with(Database.get(), repo.workspaceIds);
		if (workspaceChanges.isEmpty())
			return Conflicts.none();
		var remoteChanges = Diffs.of(repo.git, commonParent).with(commit);
		var diffs = between(workspaceChanges, remoteChanges);
		return check(diffs);
	}

	private static Conflicts localDiffs(Commit commit, Commit commonParent) throws IOException {
		var repo = Repository.get();
		var localCommit = repo.commits.get(repo.commits.resolve(Constants.LOCAL_BRANCH));
		if (localCommit == null)
			return Conflicts.none();
		var localChanges = Diffs.of(repo.git, commonParent).with(localCommit);
		if (localChanges.isEmpty())
			return Conflicts.none();
		var remoteChanges = Diffs.of(repo.git, commonParent).with(commit);
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

	private static TypeRefIdMap<ConflictResolution> solve(List<TriDiff> conflicts) {
		if (conflicts.isEmpty())
			return new TypeRefIdMap<>();
		var resolved = new TypeRefIdMap<ConflictResolution>();
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
		var descriptors = new TypeRefIdMap<RootDescriptor>();
		for (var type : ModelType.values()) {
			Daos.root(Database.get(), type).getDescriptors().forEach(d -> descriptors.put(d.type, d.refId, d));
		}
		conflicts.forEach(conflict -> {
			if (equalsDescriptor(conflict, descriptors.get(conflict))) {
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
		var remoteModel = Repository.get().datasets.parse(ref, "lastChange", "version");
		if (remoteModel == null)
			return false;
		var version = Version.fromString(string(remoteModel, "version")).getValue();
		var lastChange = date(remoteModel, "lastChange");
		return version == d.version && lastChange == d.lastChange;
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
