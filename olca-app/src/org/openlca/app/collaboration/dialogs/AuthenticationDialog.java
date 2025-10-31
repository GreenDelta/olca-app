package org.openlca.app.collaboration.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.collaboration.util.CredentialStore;
import org.openlca.app.components.AuthenticationGroup;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.collaboration.model.Credentials;
import org.openlca.util.Strings;

public class AuthenticationDialog extends FormDialog {

	private final String url;
	private final String user;
	private final String password;
	private boolean userPrompt;
	private boolean passwordPrompt;
	private boolean tokenPrompt;
	public AuthenticationGroup auth;

	private AuthenticationDialog(String url) {
		this(url, null);
	}

	private AuthenticationDialog(String url, String user) {
		this(url, user, null);
	}

	private AuthenticationDialog(String url, String user, String password) {
		super(UI.shell());
		this.url = url;
		this.user = user;
		this.password = password;
		setBlockOnOpen(true);
	}

	public static PersonIdent promptUser(String url, String user) {
		if (Strings.isNotBlank(user))
			return new PersonIdent(user, "");
		var dialog = new AuthenticationDialog(url);
		dialog.userPrompt = true;
		if (dialog.open() == AuthenticationDialog.CANCEL)
			return null;
		return new PersonIdent(dialog.auth.user(), "");
	}

	public static GitCredentialsProvider promptCredentials(String url) {
		return promptCredentials(url, null);
	}

	public static GitCredentialsProvider promptCredentials(String url, String user) {
		if (Strings.isBlank(user)) {
			user = CredentialStore.getUsername(url);
		}
		var password = CredentialStore.getPassword(url, user);
		if (Strings.isNotBlank(user) && Strings.isNotBlank(password))
			return new GitCredentialsProvider(url, user, password, null);
		var dialog = new AuthenticationDialog(url, user, password);
		dialog.passwordPrompt = true;
		if (dialog.open() == AuthenticationDialog.CANCEL)
			return null;
		user = dialog.auth.user();
		password = dialog.auth.password();
		CredentialStore.put(url, user, password);
		return new GitCredentialsProvider(url, user, password, dialog.auth.token());
	}

	public static GitCredentialsProvider promptToken(String url, String user) {
		var password = CredentialStore.getPassword(url, user);
		var dialog = new AuthenticationDialog(url, user);
		dialog.tokenPrompt = true;
		if (dialog.open() == AuthenticationDialog.CANCEL)
			return null;
		if (Strings.isBlank(user)) {
			user = dialog.auth.user();
		}
		if (Strings.isBlank(password)) {
			password = dialog.auth.password();
		}
		CredentialStore.put(url, user, password);
		return new GitCredentialsProvider(url, user, password, dialog.auth.token());
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var message = M.EnterGitCredentials + " - ";
		if (Strings.isNotBlank(user)) {
			message += user + "@";
		}
		message += url;
		var formBody = UI.header(form, form.getToolkit(), M.Authenticate, message);
		var body = UI.composite(formBody, form.getToolkit());
		UI.gridLayout(body, 1);
		UI.gridData(body, true, true).widthHint = 500;
		auth = new AuthenticationGroup(body, form.getToolkit(), SWT.FOCUSED)
				.onChange(this::updateButtons);
		if (userPrompt) {
			auth.withUser();
		} else if (passwordPrompt) {
			auth.withUser(user).withPassword();
		} else if (tokenPrompt) {
			if (Strings.isBlank(user) || Strings.isBlank(password)) {
				auth.withUser(user).withPassword();
			}
			auth.withToken();
		}
		auth.render();
		form.getForm().reflow(true);
	}

	private void updateButtons() {
		var button = getButton(IDialogConstants.OK_ID);
		if (button == null || auth == null)
			return;
		button.setEnabled(auth.isComplete());
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		var ok = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		ok.setEnabled(false);
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
			super(user, Strings.isBlank(token) ? password : password + "&token=" + token);
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
			CredentialStore.clearPassword(url, user);
			return true;
		}

		@Override
		public boolean onUnauthorized() {
			MsgBox.warning(M.NoSufficientRights);
			CredentialStore.clearUsername(url);
			return true;
		}

	}

}
