package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.collaboration.navigation.NavCache;
import org.openlca.app.collaboration.navigation.RepositoryLabel;
import org.openlca.app.collaboration.util.PathFilters;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Question;
import org.openlca.git.actions.GitStashCreate;
import org.openlca.git.model.Change;

public class DiscardAction extends Action implements INavigationAction {

	private List<INavigationElement<?>> selection;

	@Override
	public String getText() {
		return "Discard changes";
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
		if (!Question.ask("Discard changes",
				"Do you really want to discard the selected changes? This action can not be undone."))
			return;
		var repo = Repository.CURRENT;
		try {
			var selected = new ArrayList<Change>();
			PathFilters.of(selection).forEach(filter -> {
				repo.diffs.find()
						.filter(filter)
						.withDatabase()
						.stream().map(Change::new)
						.forEach(selected::add);
			});
			Actions.run(GitStashCreate.on(repo)
					.changes(selected)
					.discard());
		} catch (IOException | InvocationTargetException | InterruptedException | GitAPIException e) {
			Actions.handleException("Error discarding changes", e);
		} finally {
			Cache.evictAll();
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
