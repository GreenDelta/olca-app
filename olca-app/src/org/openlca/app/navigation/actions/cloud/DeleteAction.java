package org.openlca.app.navigation.actions.cloud;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.cloud.api.RepositoryClient;
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
			String name = client.getConfig().getRepositoryName();
			try {
				client.deleteRepository(name);
			} catch (WebRequestException e) {
				log.error("Unexpected exception when deleting repository "
						+ name, e);
			}
		});
		Database.disconnect();
		Navigator.refresh(Navigator.getNavigationRoot());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		client = Database.getRepositoryClient();
		if (client == null)
			return false;
		String owner = client.getConfig().getRepositoryOwner();
		return client.getConfig().getUsername().equals(owner);
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

}
