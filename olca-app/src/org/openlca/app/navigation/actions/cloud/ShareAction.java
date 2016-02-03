package org.openlca.app.navigation.actions.cloud;

import org.openlca.app.M;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
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
		return M.ShareRepository;
	}

	@Override
	public void run() {
		InputDialog dialog = new InputDialog(UI.shell(), M.ShareRepository,
				M.ShareDescription,
				null, null);
		if (dialog.open() != IDialogConstants.OK_ID)
			return;
		String username = dialog.getValue();
		App.runWithProgress(M.SharingRepository,
				() -> shareRepository(username));
		if (error != null) {
			Error.showBox(error.getMessage());
			error = null;
		}
	}

	private void shareRepository(String username) {
		String name = client.getConfig().getRepositoryName();
		try {
			client.shareRepositoryWith(name, username);
		} catch (WebRequestException e) {
			error = e;
		}
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
