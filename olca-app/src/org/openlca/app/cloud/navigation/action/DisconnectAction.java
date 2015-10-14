package org.openlca.app.cloud.navigation.action;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.cloud.navigation.RepositoryElement;
import org.openlca.app.cloud.navigation.RepositoryNavigator;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.actions.INavigationAction;

import com.greendelta.cloud.api.RepositoryConfig;
import com.greendelta.cloud.util.Directories;

public class DisconnectAction extends Action implements INavigationAction {

	private RepositoryConfig config;

	@Override
	public String getText() {
		return "#Disconnect from repository";
	}

	@Override
	public void run() {
		// TODO add monitor
		config.disconnect();
		File fileStorage = config.getDatabase().getFileStorageLocation();
		RepositoryNavigator.disconnect();
		Directories.delete(new File(fileStorage, "cloud/"
				+ config.getRepositoryId()));
		RepositoryNavigator.refresh();
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof RepositoryElement))
			return false;
		config = (RepositoryConfig) element.getContent();
		if (config == null)
			return false;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

}
