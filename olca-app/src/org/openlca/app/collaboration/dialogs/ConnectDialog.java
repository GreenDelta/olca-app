package org.openlca.app.collaboration.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog.GitCredentialsProvider;
import org.openlca.app.tools.authentification.AuthenticationGroup;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

public class ConnectDialog extends FormDialog {

	private LocationGroup location = new LocationGroup().withRepository().onChange(this::updateButtons);
	private AuthenticationGroup auth = new AuthenticationGroup().withUser().onChange(this::updateButtons);
	private boolean withPassword;

	public ConnectDialog() {
		super(UI.shell());
		setBlockOnOpen(true);
	}

	public ConnectDialog withPassword() {
		this.withPassword = true;
		return this;
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var formBody = UI.header(form, form.getToolkit(),
				"Connect Git repository",
				"Enter the location of the Git repository.");
		var body = UI.composite(formBody, form.getToolkit());
		UI.gridLayout(body, 1);
		UI.gridData(body, true, true).widthHint = 500;
		location.render(body, form.getToolkit());
		if (withPassword) {
			auth.withPassword();
		}
		auth.render(body, form.getToolkit(), SWT.NONE);
		form.getForm().reflow(true);
	}

	private void updateButtons() {
		var enabled = !Strings.nullOrEmpty(location.url())
				&& !Strings.nullOrEmpty(auth.user())
				&& (!withPassword || !Strings.nullOrEmpty(auth.password()));
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		var commit = createButton(parent, IDialogConstants.OK_ID, M.Connect, true);
		commit.setEnabled(false);
		setButtonLayoutData(commit);
	}

	public String url() {
		return location.url();
	}

	public String user() {
		return auth.user();
	}

	public GitCredentialsProvider credentials() {
		return new GitCredentialsProvider(auth.user(), auth.password());
	}

}
