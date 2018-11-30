package org.openlca.app.navigation.actions.cloud;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.Announcements;
import org.openlca.app.cloud.TokenDialog;
import org.openlca.app.cloud.index.Reindexing;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.cloud.ui.preferences.CloudConfiguration;
import org.openlca.app.cloud.ui.preferences.CloudConfigurations;
import org.openlca.app.cloud.ui.preferences.CloudPreferencePage;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.cloud.api.CredentialSupplier;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.api.RepositoryConfig;

import com.google.common.base.Strings;

public class ConnectAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return M.ConnectToRepository;
	}

	@Override
	public void run() {
		Runner runner = new Runner();
		if (!runner.run())
			return;
		if (runner.error != null)
			Error.showBox(runner.error.getMessage());
		else {
			App.runWithProgress(M.RebuildingIndex, Reindexing::execute);
			Navigator.refresh(Navigator.getNavigationRoot());
		}
	}

	private class Runner {

		private Exception error;

		private boolean run() {
			InputDialog dialog = new InputDialog();
			if (dialog.open() != Dialog.OK)
				return false;
			RepositoryConfig config = dialog.createConfig();
			String text = M.ConnectingToRepository + config.getServerUrl() + " " + config.repositoryId;
			App.runWithProgress(text, () -> connect(config));
			HistoryView.refresh();
			return true;
		}

		private void connect(RepositoryConfig config) {
			RepositoryClient client = new RepositoryClient(config);
			try {
				if (!client.hasAccess(config.repositoryId)) {
					error = new Exception(M.NoAccessToRepository);
				}
			} catch (Exception e) {
				error = e;
			}
			if (error == null) {
				Database.connect(client);
				Announcements.check(client);
			} else {
				config.disconnect();
			}
		}

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
			Composite container = new Composite(parent, SWT.NONE);
			UI.gridLayout(container, 3);
			UI.formLabel(container, M.ServerUrl);
			ConfigViewer configViewer = new ConfigViewer(container);
			configViewer.setInput(CloudConfigurations.get());
			configViewer.addSelectionChangedListener((config) -> {
				if (config == null) {
					serverUrl = null;
					username = null;
					password = null;
					return;
				}
				serverUrl = config.getUrl();
				username = config.getUser();
				password = config.getPassword();
				checkValid();
			});
			configViewer.select(CloudConfigurations.getDefault());
			Hyperlink editConfig = UI.formLink(container, M.Edit);
			editConfig.setForeground(Colors.linkBlue());
			editConfig.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null, CloudPreferencePage.ID,
							null, null);
					dialog.setBlockOnOpen(true);
					dialog.open();
					configViewer.setInput(CloudConfigurations.get());
					configViewer.select(CloudConfigurations.getDefault());
				}
			});
			Text repoText = UI.formText(container, M.RepositoryPath);
			repoText.addModifyListener((event) -> {
				repositoryId = repoText.getText();
				if (repositoryId != null)
					repositoryId.trim();
				checkValid();
			});
			if (username != null) {
				repoText.setText(username + "/" + Database.get().getName());
			}
			return container;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			checkValid();
		}

		private void checkValid() {
			boolean valid = true;
			if (Strings.isNullOrEmpty(serverUrl))
				valid = false;
			if (Strings.isNullOrEmpty(username))
				valid = false;
			if (Strings.isNullOrEmpty(password))
				valid = false;
			if (Strings.isNullOrEmpty(repositoryId))
				valid = false;
			Button button = getButton(IDialogConstants.OK_ID);
			if (button != null)
				button.setEnabled(valid);
		}

		private RepositoryConfig createConfig() {
			CredentialSupplier credentials = new CredentialSupplier(username, password);
			credentials.setTokenSupplier(TokenDialog::prompt);
			RepositoryConfig config = RepositoryConfig.connect(Database.get(), serverUrl + "/ws", repositoryId,
					credentials);
			return config;
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

	private class ConfigViewer extends AbstractComboViewer<CloudConfiguration> {

		protected ConfigViewer(Composite parent) {
			super(parent);
		}

		@Override
		public Class<CloudConfiguration> getType() {
			return CloudConfiguration.class;
		}

	}

}
