package org.openlca.app.navigation.actions.cloud;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.cloud.ui.preferences.CloudPreference;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.api.RepositoryConfig;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.util.WebRequests.WebRequestException;

public class ConnectAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return M.ConnectToRepository;
	}

	@Override
	public void run() {
		Runner runner = new Runner();
		runner.run();
		if (runner.error != null)
			Error.showBox(runner.error.getMessage());
		else {
			indexDatabase();
			Navigator.refresh(Navigator.getNavigationRoot());
		}
	}

	private class Runner {

		private Exception error;

		private void run() {
			InputDialog dialog = new InputDialog();
			if (dialog.open() != Dialog.OK)
				return;
			RepositoryConfig config = dialog.createConfig();
			String text = M.ConnectingToRepository + config.getServerUrl() + " " + config.getRepositoryId();
			App.runWithProgress(text, () -> connect(config));
			HistoryView.refresh();
		}

		private void connect(RepositoryConfig config) {
			RepositoryClient client = new RepositoryClient(config);
			try {
				if (!client.hasAccess(config.getRepositoryId())) {
					error = new Exception(M.NoAccessToRepository);
				}
			} catch (WebRequestException e) {
				error = e;
			}
			if (error == null)
				Database.connect(config);
			else
				config.disconnect();
		}

	}

	private void indexDatabase() {
		IDatabaseConfiguration db = Database.getActiveConfiguration();
		INavigationElement<?> element = Navigator.findElement(db);
		DiffIndex index = Database.getDiffIndex();
		indexElement(index, element);
		index.commit();
	}

	private void indexElement(DiffIndex index, INavigationElement<?> element) {
		long id = 0;
		if (element instanceof CategoryElement)
			id = ((CategoryElement) element).getContent().getId();
		if (element instanceof ModelElement)
			id = ((ModelElement) element).getContent().getId();
		if (id != 0l) {
			Dataset dataset = CloudUtil.toDataset(element);
			index.add(dataset, id);
			index.update(dataset, DiffType.NEW);
		}
		for (INavigationElement<?> child : element.getChildren())
			indexElement(index, child);
	}

	private class InputDialog extends Dialog {

		private String serverUrl;
		private String username;
		private String password;
		private String repositoryId;

		protected InputDialog() {
			super(UI.shell());
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite container = UI.formComposite(parent);
			Text serverText = UI.formText(container, M.ServerUrl);
			((GridData) serverText.getLayoutData()).minimumWidth = 250;
			serverText.addModifyListener((event) -> {
				serverUrl = serverText.getText();
				if (serverUrl != null)
					serverUrl.trim();
			});
			Text usernameText = UI.formText(container, M.Username);
			usernameText.addModifyListener((event) -> {
				username = usernameText.getText();
				if (username != null)
					username.trim();
			});
			Text passwordText = UI.formText(container, M.Password,
					SWT.PASSWORD);
			passwordText.addModifyListener((event) -> {
				password = passwordText.getText();
			});
			Text repoText = UI.formText(container, M.RepositoryId);
			repoText.addModifyListener((event) -> {
				repositoryId = repoText.getText();
				if (repositoryId != null)
					repositoryId.trim();
			});
			serverText.setText(CloudPreference.getDefaultHost());
			usernameText.setText(CloudPreference.getDefaultUser());
			passwordText.setText(CloudPreference.getDefaultPass());
			return container;
		}

		private RepositoryConfig createConfig() {
			return RepositoryConfig.connect(Database.get(), serverUrl + "/ws",
					repositoryId, username, password);
		}
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement elem = (DatabaseElement) element;
		if (!Database.isActive(elem.getContent()))
			return false;
		RepositoryClient client = Database.getRepositoryClient();
		if (client != null)
			return false;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

}
