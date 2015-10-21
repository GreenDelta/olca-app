package org.openlca.app.cloud.navigation.action;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.openlca.app.App;
import org.openlca.app.cloud.navigation.RepositoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.util.WebRequests.WebRequestException;

public class ShareAction extends Action implements INavigationAction {

	private RepositoryClient client;
	private Exception error;
	
	@Override
	public String getText() {
		return "#Share repository...";
	}

	@Override
	public void run() {
		InputDialog dialog = new InputDialog(UI.shell(), "#Share repository",
				"#Specify the user you want to share the repository with",
				null, null);
		if (dialog.open() != IDialogConstants.OK_ID)
			return;
		String username = dialog.getValue();		
		App.runWithProgress("#Sharing repository", () -> {
			String name = client.getConfig().getRepositoryId().split("/")[1];
			try {
				client.shareRepositoryWith(name, username);
			} catch (WebRequestException e) {
				error = e;
			}
		});
		if (error != null) {
			Error.showBox(error.getMessage());
			error = null;
		}
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof RepositoryElement))
			return false;
		client = (RepositoryClient) element.getContent();
		if (client == null)
			return false;
		String owner = client.getConfig().getRepositoryId().split("/")[0];
		return client.getConfig().getUsername().equals(owner);
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

}
