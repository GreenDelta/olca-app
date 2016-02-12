package org.openlca.app.navigation.actions.cloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffType;
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
			storeConnectionData(config.getServerUrl(), config.getUsername());
			String text = M.ConnectingToRepository + config.getServerUrl()
					+ " " + config.getRepositoryId();
			App.runWithProgress(text, () -> connect(config));
		}

		private void connect(RepositoryConfig config) {
			RepositoryClient client = new RepositoryClient(config);
			try {
				String owner = config.getRepositoryOwner();
				String repositoryName = config.getRepositoryName();
				if (owner.equals(config.getUsername())) {
					if (!client.repositoryExists(repositoryName))
						client.createRepository(repositoryName);
				} else {
					if (!client.hasAccess(config.getRepositoryId())) {
						error = new Exception(
								M.NoAccessToRepository);
						config.disconnect();
						return;
					}
				}
			} catch (WebRequestException e) {
				error = e;
				config.disconnect();
				return;
			}
			Database.connect(config);
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
			serverText.addModifyListener((event) -> {
				serverUrl = serverText.getText();
			});
			Text usernameText = UI.formText(container, M.Username);
			usernameText.addModifyListener((event) -> {
				username = usernameText.getText();
			});
			Text passwordText = UI.formText(container, M.Password,
					SWT.PASSWORD);
			passwordText.addModifyListener((event) -> {
				password = passwordText.getText();
			});
			Text repoText = UI.formText(container, M.RepositoryId);
			repoText.addModifyListener((event) -> {
				repositoryId = repoText.getText();
			});
			Properties config = loadConnectionData();
			if (config.containsKey("server"))
				serverText.setText(config.getProperty("server"));
			if (config.containsKey("username")) {
				String username = config.getProperty("username");
				usernameText.setText(username);
				repoText.setText(username + "/" + Database.get().getName());
			}
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

	private void storeConnectionData(String server, String username) {
		File config = getConfigFile();
		Properties properties = new Properties();
		properties.setProperty("server", server);
		properties.setProperty("username", username);
		try (FileOutputStream out = new FileOutputStream(config)) {
			if (!config.exists())
				config.createNewFile();
			properties.store(out, null);
		} catch (IOException e) {
			// fail silently, this is just for convenience
		}
	}

	private Properties loadConnectionData() {
		File config = getConfigFile();
		Properties properties = new Properties();
		try (FileInputStream in = new FileInputStream(config)) {
			properties.load(in);
		} catch (IOException e) {
			// fail silently, this is just for convenience
		}
		return properties;
	}

	private File getConfigFile() {
		File file = Database.get().getFileStorageLocation();
		return new File(file, "cloud_config.properties");
	}

}
