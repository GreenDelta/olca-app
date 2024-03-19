package org.openlca.app.collaboration.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.collaboration.util.CredentialStore;
import org.openlca.app.tools.authentification.AuthenticationGroup;
import org.openlca.app.util.UI;
import org.openlca.collaboration.model.Credentials;
import org.openlca.util.Strings;

public class AuthenticationDialog extends FormDialog {

	public final AuthenticationGroup auth = new AuthenticationGroup();
	private final String url;
	private final String user;

	public AuthenticationDialog(String url) {
		this(url, null);
	}

	public AuthenticationDialog(String url, String user) {
		super(UI.shell());
		this.url = url;
		this.user = user;
		setBlockOnOpen(true);
	}

	public static GitCredentialsProvider promptCredentials(String url) {
		return promptCredentials(url, null);
	}

	public static GitCredentialsProvider promptCredentials(String url, String user) {
		if (Strings.nullOrEmpty(user)) {
			user = CredentialStore.getUsername(url);
		}
		var password = CredentialStore.getPassword(url, user);
		if (!Strings.nullOrEmpty(user) && !Strings.nullOrEmpty(password))
			return new GitCredentialsProvider(url, user, password, null);
		var dialog = new AuthenticationDialog(url);
		var auth = dialog.auth;
		auth.withUser(user).withPassword(password);
		if (dialog.open() == AuthenticationDialog.CANCEL)
			return null;
		user = auth.user();
		password = auth.password();
		if (auth == null || Strings.nullOrEmpty(user) || Strings.nullOrEmpty(password))
			return null;
		var token = auth.token();
		CredentialStore.put(url, user, password);
		return new GitCredentialsProvider(url, user, password, token);
	}

	public static PersonIdent promptUser(String url, String user) {
		if (!Strings.nullOrEmpty(user))
			return new PersonIdent(user, "");
		var dialog = new AuthenticationDialog(url);
		var auth = dialog.auth;
		auth.withUser(user);
		if (dialog.open() == AuthenticationDialog.CANCEL)
			return null;
		user = auth.user();
		return new PersonIdent(user, "");
	}

	public static GitCredentialsProvider promptToken(String url, String user) {
		var dialog = new AuthenticationDialog(url, user);
		var auth = dialog.auth;
		if (Strings.nullOrEmpty(user)) {
			auth.withUser();
		}
		var password = CredentialStore.getPassword(url, user);
		if (Strings.nullOrEmpty(password)) {
			auth.withPassword();
		}
		auth.withToken();
		if (dialog.open() == AuthenticationDialog.CANCEL)
			return null;
		return new GitCredentialsProvider(url, user, password, auth.token());
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var message = "Please enter your credentials for " + url;
		if (!Strings.nullOrEmpty(user)) {
			message += " and user " + user;
		}
		var formBody = UI.header(form, form.getToolkit(), "Authenticate", message);
		var body = UI.composite(formBody, form.getToolkit());
		UI.gridLayout(body, 1);
		UI.gridData(body, true, true).widthHint = 500;
		auth.onChange(this::updateButtons)
				.render(body, form.getToolkit(), SWT.FOCUSED);
		form.getForm().reflow(true);
	}

	private void updateButtons() {
		getButton(IDialogConstants.OK_ID).setEnabled(auth.isComplete());
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		var ok = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		ok.setEnabled(auth.isComplete());
		setButtonLayoutData(ok);
	}

	public AuthenticationGroup values() {
		return auth;
	}

	public static class GitCredentialsProvider extends UsernamePasswordCredentialsProvider implements Credentials {

		private final String url;
		public final String user;
		public final PersonIdent ident;
		public final String password;
		public final String token;

		private GitCredentialsProvider(String url, String user, String password, String token) {
			super(user, Strings.nullOrEmpty(token) ? password : password + "&token=" + token);
			this.url = url;
			this.user = user;
			this.ident = new PersonIdent(user, "");
			this.password = password;
			this.token = token;
		}

		@Override
		public String username() {
			return user;
		}

		@Override
		public String password() {
			return password;
		}

		@Override
		public String token() {
			return token;
		}

		@Override
		public String promptToken() {
			var auth = AuthenticationDialog.promptToken(url, user);
			return auth != null && auth.token != null
					? auth.token
					: null;
		}

		@Override
		public boolean onUnauthenticated() {
			return promptCredentials(url, user) != null;
		}

		@Override
		public boolean onUnauthorized() {
			return promptCredentials(url, user) != null;
		}

	}

}
