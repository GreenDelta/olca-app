package org.openlca.app.licence;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.components.AuthenticationGroup;
import org.openlca.app.util.UI;
import org.openlca.license.access.Credentials;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationDialog extends FormDialog {

	static Logger log = LoggerFactory.getLogger(AuthenticationDialog.class);
	private final String library;
	private AuthenticationGroup auth;

	public AuthenticationDialog(String library) {
		super(UI.shell());
		this.library = library;
		setBlockOnOpen(true);
	}

	public static Credentials promptCredentials(String library) {
		var dialog = new AuthenticationDialog(library);
		if (dialog.open() == AuthenticationDialog.CANCEL)
			return null;
		if (Strings.nullOrEmpty(dialog.auth.user())
				|| Strings.nullOrEmpty(dialog.auth.password()))
			return null;
		return new Credentials(dialog.auth.user(), dialog.auth.password().toCharArray());
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var formBody = UI.header(form, form.getToolkit(),
				M.Authentication + " - " + library,
				M.PleaseEnterYourCredentialsLibrary);
		var body = UI.composite(formBody, form.getToolkit());
		UI.gridLayout(body, 1);
		UI.gridData(body, true, true).widthHint = 500;
		auth = new AuthenticationGroup(body, form.getToolkit(), SWT.FOCUSED)
				.withUser().withPassword()
				.onChange(this::updateButtons);
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
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		var ok = createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
		ok.setEnabled(false);
		setButtonLayoutData(ok);
	}

}
