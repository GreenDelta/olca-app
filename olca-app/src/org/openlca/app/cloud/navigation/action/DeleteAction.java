package org.openlca.app.cloud.navigation.action;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.cloud.navigation.RepositoryElement;
import org.openlca.app.cloud.navigation.RepositoryNavigator;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.util.Directories;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteAction extends Action implements INavigationAction {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private RepositoryClient client;

	@Override
	public String getText() {
		return "#Delete repository";
	}

	@Override
	public void run() {
		App.runWithProgress("#Deleting repository", () -> {
			String name = client.getConfig().getRepositoryId().split("/")[1];
			try {
				client.deleteRepository(name);
			} catch (WebRequestException e) {
				log.error("Unexpected exception when deleting repository "
						+ name, e);
			}
		});
		client.getConfig().disconnect();
		File fileStorage = client.getConfig().getDatabase()
				.getFileStorageLocation();
		RepositoryNavigator.disconnect();
		Directories.delete(new File(fileStorage, "cloud/"
				+ client.getConfig().getRepositoryId()));
		RepositoryNavigator.refresh();
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof RepositoryElement))
			return false;
		client = (RepositoryClient) element.getContent();
		if (client == null)
			return false;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

}
