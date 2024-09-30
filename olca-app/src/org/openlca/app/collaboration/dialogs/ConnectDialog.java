package org.openlca.app.collaboration.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.collaboration.browse.ServerNavigator;
import org.openlca.app.collaboration.navigation.ServerConfigurations;
import org.openlca.app.collaboration.navigation.ServerConfigurations.ServerConfig;
import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.collaboration.util.CredentialStore;
import org.openlca.app.components.AuthenticationGroup;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

public class ConnectDialog extends FormDialog {

	private LocationGroup location;
	private RepositorySelect repository;
	private AuthenticationGroup auth;
	private boolean fromServer;

	public ConnectDialog() {
		super(UI.shell());
		setBlockOnOpen(true);
	}

	@Override
	public int open() {
		var result = super.open();
		if (result != OK)
			return result;
		if (!fromServer) {
			CredentialStore.put(location.serverUrl(), auth.user(), auth.password());
			CollaborationPreference.storeConnection(location.storeConnection());
			if (location.storeConnection()) {
				ServerConfigurations.put(new ServerConfig(location.serverUrl(), auth.user()));
				ServerNavigator.refresh();
			}
		}
		return OK;
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var toolkit = form.getToolkit();
		var formBody = UI.header(form, toolkit,
				M.ConnectGitRepo,
				M.EnterGitLocation);
		var body = UI.composite(formBody, toolkit);
		UI.gridLayout(body, 1);
		UI.gridData(body, true, true);
		var servers = ServerConfigurations.get();
		if (!servers.isEmpty()) {
			UI.radioGroup(body, toolkit, new String[] { M.NewLocation, M.SelectExisting }, selection -> {
				fromServer = selection == 1;
				setVisible(location, !fromServer);
				setVisible(auth, !fromServer);
				setVisible(repository, fromServer);
				if (form != null) {
					form.getForm().reflow(true);
				}
				getShell().pack(true);
			});
		}
		location = new LocationGroup(body, form.getToolkit())
				.withRepository().withStoreOption()
				.onChange(this::update);
		location.render();
		if (!servers.isEmpty()) {
			repository = new RepositorySelect(body, form)
					.onChange(this::update);
			repository.render();
			setVisible(repository, false);
		}
		auth = new AuthenticationGroup(body, toolkit, SWT.NONE)
				.withUser().withPassword()
				.onChange(this::update);
		auth.render();
		form.getForm().reflow(true);
	}

	private void setVisible(Control c, boolean value) {
		var data = (GridData) c.getLayoutData();
		data.exclude = !value;
		c.setVisible(value);
	}

	private void update() {
		var button = getButton(IDialogConstants.OK_ID);
		if (button == null)
			return;
		var enabled = fromServer
				? !Strings.nullOrEmpty(repository.url())
				: !Strings.nullOrEmpty(location.url())
						&& !Strings.nullOrEmpty(auth.user())
						&& !Strings.nullOrEmpty(auth.password());
		button.setEnabled(enabled);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		var commit = createButton(parent, IDialogConstants.OK_ID, M.Connect, true);
		commit.setEnabled(false);
		setButtonLayoutData(commit);
	}

	public String url() {
		if (fromServer)
			return repository.url();
		return location.url();
	}

	public String user() {
		if (fromServer)
			return CredentialStore.getUsername(repository.server.url());
		return auth.user();
	}

	public String password() {
		if (fromServer) {
			var user = CredentialStore.getUsername(repository.server.url());
			return CredentialStore.getPassword(repository.server.url(), user);
		}
		return auth.password();
	}

}
