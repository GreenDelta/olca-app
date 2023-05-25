package org.openlca.app.collaboration.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.db.Repository;
import org.openlca.app.tools.authentification.AuthenticationGroup;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

public class AuthenticationDialog extends FormDialog {

	private final AuthenticationGroup auth = new AuthenticationGroup();
	private final String url;

	private AuthenticationDialog() {
		this("");
	}

	private AuthenticationDialog(String url) {
		super(UI.shell());
		this.url = url;
		setBlockOnOpen(true);
	}

	public static GitCredentialsProvider promptCredentials(String url) {
		return promptCredentials(null, url, false);
	}

	public static GitCredentialsProvider promptCredentials(Repository repo) {
		var url = repo.client.serverUrl + "/" + repo.client.repositoryId;
		return promptCredentials(repo, url, false);
	}

	public static GitCredentialsProvider forcePromptCredentials(String url) {
		return promptCredentials(null, url, true);
	}

	public static GitCredentialsProvider forcePromptCredentials(Repository repo) {
		var url = repo.client.serverUrl + "/" + repo.client.repositoryId;
		return promptCredentials(repo, url, true);
	}

	private static GitCredentialsProvider promptCredentials(Repository repo, String url, boolean forceAll) {
		var dialog = new AuthenticationDialog(url);
		var auth = dialog.auth;
		var user = repo != null ? repo.user() : null;
		var password = repo != null ? repo.password() : null;
		var useTwoFactorAuth = repo != null ? repo.useTwoFactorAuth() : false;
		auth.withUser(user).withPassword(password);
		if (useTwoFactorAuth) {
			auth.withToken();
		}
		if (!Strings.nullOrEmpty(user) && !Strings.nullOrEmpty(password) && !forceAll) {
			if (!useTwoFactorAuth)
				return new GitCredentialsProvider(user, password);
			if (!forceAll)
				return promptToken(user, password);
		}
		if (dialog.open() == AuthenticationDialog.CANCEL)
			return null;
		if (auth == null || Strings.nullOrEmpty(auth.user()) || Strings.nullOrEmpty(auth.password()))
			return null;
		user = auth.user();
		password = auth.password();
		var token = auth.token();
		if (repo != null) {
			repo.user(user);
			repo.password(password);
		}
		return new GitCredentialsProvider(user, password, token);
	}

	public static PersonIdent promptUser(Repository repo) {
		var user = repo != null ? repo.user() : null;
		if (!Strings.nullOrEmpty(user))
			return new PersonIdent(user, "");
		var dialog = new AuthenticationDialog();
		var auth = dialog.auth;
		auth.withUser(user);
		if (dialog.open() == AuthenticationDialog.CANCEL)
			return null;
		user = auth.user();
		if (repo != null) {
			repo.user(user);
		}
		return new PersonIdent(user, "");
	}

	public static GitCredentialsProvider promptToken(Repository repo) {
		if (repo == null)
			return null;
		return promptToken(repo.user(), repo.password());
	}

	public static GitCredentialsProvider promptToken(String user, String password) {
		var dialog = new AuthenticationDialog();
		var auth = dialog.auth;
		auth.withToken();
		if (dialog.open() == AuthenticationDialog.CANCEL)
			return null;
		return new GitCredentialsProvider(user, password, auth.token());
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var formBody = UI.header(form, form.getToolkit(),
				"Authenticate " + url,
				"Enter your credentials for the Git repository.");
		var body = UI.composite(formBody, form.getToolkit());
		UI.gridLayout(body,  1);
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

	public static class GitCredentialsProvider extends UsernamePasswordCredentialsProvider {

		public final String user;
		public final PersonIdent ident;
		public final String password;
		public final String token;

		GitCredentialsProvider(String user, String password) {
			this(user, password, null);
		}

		private GitCredentialsProvider(String user, String password, String token) {
			super(user, Strings.nullOrEmpty(token) ? password : password + "&token=" + token);
			this.user = user;
			this.ident = new PersonIdent(user, "");
			this.password = password;
			this.token = token;
		}

	}

}
