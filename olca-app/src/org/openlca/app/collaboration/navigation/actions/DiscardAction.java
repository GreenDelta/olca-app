package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.AppContext;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.navigation.NavCache;
import org.openlca.app.collaboration.navigation.RepositoryLabel;
import org.openlca.app.collaboration.util.PathFilters;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Question;
import org.openlca.git.actions.GitReset;

class DiscardAction extends Action implements INavigationAction {

	private List<INavigationElement<?>> selection;

	@Override
	public String getText() {
		return M.DiscardChanges;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.DELETE.descriptor();
	}

	@Override
	public boolean isEnabled() {
		for (var selected : selection)
			if (selected instanceof DatabaseElement && NavCache.get().hasChanges())
				return true;
			else if (RepositoryLabel.hasChanged(selected))
				return true;
		return false;
	}

	@Override
	public void run() {
		if (!Question.ask(M.DiscardChangesQ, M.DiscardChangesQuestion))
			return;
		try {
			var repo = Repository.CURRENT;
			var head = repo.commits.head();
			var selected = PathFilters.of(selection).stream()
					.map(filter -> repo.diffs.find()
							.filter(filter)
							.commit(head)
							.withDatabase())
					.flatMap(List::stream)
					.collect(Collectors.toList());
			Actions.run(GitReset.on(repo)
					.to(head)
					.changes(selected)
					.resolveLibrariesWith(WorkspaceLibraryResolver.forCommit(head)));
		} catch (IOException | InvocationTargetException | InterruptedException | GitAPIException e) {
			Actions.handleException("Error discarding changes", e);
		} finally {
			AppContext.evictAll();
			Actions.refresh();
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (!Repository.isConnected())
			return false;
		this.selection = selection;
		return true;
	}

}
