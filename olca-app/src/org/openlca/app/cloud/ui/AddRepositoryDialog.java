package org.openlca.app.cloud.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.TokenDialog;
import org.openlca.app.cloud.ui.preferences.CloudConfiguration;
import org.openlca.app.cloud.ui.preferences.CloudConfigurations;
import org.openlca.app.cloud.ui.preferences.CloudPreferencePage;
import org.openlca.app.db.Database;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.cloud.api.CredentialSupplier;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.api.RepositoryConfig;

import com.google.common.base.Strings;

class AddRepositoryDialog extends FormDialog {

	private String serverUrl;
	private String username;
	private String password;
	private String repositoryId;
	private ConfigViewer configViewer;
	private RepositoryViewer repositoryViewer;
	private List<String> repositories;

	AddRepositoryDialog() {
		super(UI.shell());
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = mform.getForm();
		FormToolkit toolkit = mform.getToolkit();
		UI.formHeader(mform, M.AddNewRepository);
		Composite body = UI.formBody(form, toolkit);
		UI.gridLayout(body, 3);
		createConfigViewer(body, toolkit);
		createRepositoryViewer(body, toolkit);
		initConfigViewer();

	}

	private void createConfigViewer(Composite container, FormToolkit toolkit) {
		UI.formLabel(container, toolkit, M.ServerUrl);
		configViewer = new ConfigViewer(container);
		configViewer.setInput(CloudConfigurations.get());
		Hyperlink editConfig = UI.formLink(container, toolkit, M.Edit);
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
	}

	private void createRepositoryViewer(Composite container, FormToolkit toolkit) {
		UI.formLabel(container, toolkit, M.RepositoryPath);
		repositoryViewer = new RepositoryViewer(container);
		repositoryViewer.addSelectionChangedListener((repository) -> {
			repositoryId = repository;
			if (repositoryId != null)
				repositoryId = repositoryId.trim();
			checkValid();
		});
	}

	private void initConfigViewer() {
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
			loadRepositories();
			checkValid();
		});
		configViewer.select(CloudConfigurations.getDefault());
	}

	private void loadRepositories() {
		App.runWithProgress(M.LoadingRepositoryList, () -> {
			RepositoryConfig config = new RepositoryConfig(null, getBaseUrl(), null);
			config.credentials = createCredentials();
			RepositoryClient client = new RepositoryClient(config);
			try {
				repositories = client.listRepositories();
			} catch (Exception e) {
				repositories = new ArrayList<>();
			}
		}, () -> {
			repositoryViewer.setInput(repositories);
			if (!repositories.isEmpty()) {
				repositoryViewer.selectFirst();
			}
		});
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

	private String getBaseUrl() {
		String baseUrl = serverUrl;
		if (!baseUrl.endsWith("/")) {
			baseUrl += "/";
		}
		return baseUrl + "ws";
	}

	private CredentialSupplier createCredentials() {
		CredentialSupplier credentials = new CredentialSupplier(username, password);
		credentials.setTokenSupplier(TokenDialog::prompt);
		return credentials;
	}

	RepositoryConfig saveConfig() {
		return RepositoryConfig.add(Database.get(), getBaseUrl(), repositoryId, createCredentials());
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

	private class RepositoryViewer extends AbstractComboViewer<String> {

		protected RepositoryViewer(Composite parent) {
			super(parent);
		}

		@Override
		public Class<String> getType() {
			return String.class;
		}

	}
}
