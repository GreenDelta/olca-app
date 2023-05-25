package org.openlca.app.licence;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.tools.authentification.AuthenticationGroup;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

public class AuthenticationDialog extends FormDialog {

	private final AuthenticationGroup auth = new AuthenticationGroup();
	private final String library;

	public AuthenticationDialog(String library) {
		super(UI.shell());
		this.library = library;
		setBlockOnOpen(true);
	}

	static LibraryCredentialsProvider promptCredentials(String library) {
		var dialog = new AuthenticationDialog(library);
		var auth = dialog.auth;
		auth.withUser().withPassword();
		if (dialog.open() == AuthenticationDialog.CANCEL)
			return null;
		if (Strings.nullOrEmpty(auth.user())
				|| Strings.nullOrEmpty(auth.password()))
			return null;
		return new LibraryCredentialsProvider(auth.user(), auth.password());
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var formBody = UI.header(form, form.getToolkit(),
				"Authenticate " + library,
				"Enter your credentials for the library.");
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
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		var ok = createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
		ok.setEnabled(auth.isComplete());
		setButtonLayoutData(ok);
	}

	public static class LibraryCredentialsProvider extends
			UsernamePasswordCredentialsProvider {

		public final String user;
		public final PersonIdent ident;
		public final String password;

		private LibraryCredentialsProvider(String user, String password) {
			super(user, password);
			this.user = user;
			this.ident = new PersonIdent(user, "");
			this.password = password;
		}

	}

}
