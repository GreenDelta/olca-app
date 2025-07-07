package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.util.Question;
import org.openlca.git.actions.GitStashApply;
import org.openlca.git.util.Constants;

class Stash {

	static void askToApply() throws InvocationTargetException, GitAPIException, IOException, InterruptedException {
		var answers = new String[] { M.No, M.Yes };
		var result = Question.ask(M.ApplyStashedChanges,
				M.ApplyStashedChangesQuestion,
				answers);
		if (result == 0)
			return;
		applyOn(Repository.get());
	}

	static boolean applyOn(Repository repo) throws GitAPIException, InvocationTargetException, IOException, InterruptedException {
		if (repo == null)
			return false;
		var dependencyResolver = WorkspaceDepencencyResolver.forStash(repo);
		if (dependencyResolver == null)
			return false;
		var conflictResolutions = ConflictResolver.resolve(repo, Constants.STASH_REF);
		if (conflictResolutions == null)
			return false;
		Actions.run(GitStashApply.on(repo)
				.resolveConflictsWith(conflictResolutions)
				.resolveDependenciesWith(dependencyResolver));
		return true;
	}
	
}
