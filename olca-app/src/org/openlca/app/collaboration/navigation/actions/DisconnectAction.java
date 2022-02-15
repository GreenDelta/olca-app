package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.collaboration.api.RepositoryConfig;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;

public class DisconnectAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return "Disonnect...";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.DISCONNECT_REPOSITORY.descriptor();
	}

	@Override
	public void run() {
		try {
			Repository.disconnect();
			var gitDir = RepositoryConfig.getGirDir(Database.get());
			FileUtils.deleteDirectory(gitDir);
		} catch (IOException e) {
			ErrorReporter.on("failed to delete git directory", e);
		} finally {
			Actions.refresh();
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement))
			return false;
		var elem = (DatabaseElement) first;
		if (!Database.isActive(elem.getContent()))
			return false;
		return Repository.isConnected();
	}

}
