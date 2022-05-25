package org.openlca.app.collaboration.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.db.Repository;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

public class AuthenticationDialog extends FormDialog {

	private final AuthenticationGroup auth = new AuthenticationGroup();

	private AuthenticationDialog() {
		super(UI.shell());
		setBlockOnOpen(true);
	}

	public static GitCredentialsProvider promptCredentials() {
		var repo = Repository.get();
		var dialog = new AuthenticationDialog();
		var auth = dialog.auth;
		var user = repo.user();
		var password = repo.password();
		var useTwoFactorAuth = repo.useTwoFactorAuth();
		auth.withUser(repo.user()).withPassword(repo.password());
		if (useTwoFactorAuth) {
			auth.withToken();
		}
		if (!useTwoFactorAuth && !Strings.nullOrEmpty(user) && !Strings.nullOrEmpty(password))
			return new GitCredentialsProvider(user, password);
		if (dialog.open() == AuthenticationDialog.CANCEL)
			return null;
		if (auth == null || Strings.nullOrEmpty(auth.user()) || Strings.nullOrEmpty(auth.password()))
			return null;
		user = auth.user();
		password = auth.password();
		var token = auth.token();
		repo.setUser(user);
		repo.setPassword(password);
		return new GitCredentialsProvider(user, password, token);
	}

	public static PersonIdent promptUser() {
		var repo = Repository.get();
		var user = repo.user();
		if (!Strings.nullOrEmpty(user))
			return new PersonIdent(user, "");
		var dialog = new AuthenticationDialog();
		var auth = dialog.auth;
		auth.withUser(user);
		if (dialog.open() == AuthenticationDialog.CANCEL)
			return null;
		user = auth.user();
		repo.setUser(user);
		return new PersonIdent(user, "");
	}

	public static GitCredentialsProvider promptToken() {
		var dialog = new AuthenticationDialog();
		var auth = dialog.auth;
		auth.withToken();
		if (dialog.open() == AuthenticationDialog.CANCEL)
			return null;
		var repo = Repository.get();
		return new GitCredentialsProvider(repo.user(), repo.password(), auth.token());
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var formBody = UI.formWizardHeader(form, form.getToolkit(),
				"Authenticate",
				"Enter your credentials for the Git repository.");
		var body = new Composite(formBody, SWT.NONE);
		UI.gridLayout(body, 1);
		UI.gridData(body, true, true).widthHint = 500;
		auth.onChange(this::updateButtons).render(body, SWT.FOCUSED);
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
