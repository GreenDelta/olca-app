package org.openlca.app.ilcd_network;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.openlca.app.Messages;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.util.Dialog;
import org.openlca.ilcd.io.Authentication;
import org.openlca.ilcd.io.NetworkClient;

public class PreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	private static final String ID = "org.openlca.ilcd.network.rcp.ui.PreferencePage";

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
		StringFieldEditor urlEditor = new StringFieldEditor(Preference.URL,
				Messages.URL, parent);
		this.addField(urlEditor);
		StringFieldEditor userEditor = new StringFieldEditor(Preference.USER,
				Messages.User, parent);
		this.addField(userEditor);
		StringFieldEditor passwordEditor = new StringFieldEditor(
				Preference.PASSWORD, Messages.Password, parent);
		passwordEditor.getTextControl(parent).setEchoChar('*');
		this.addField(passwordEditor);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		getApplyButton().setText(Messages.Test);
		setImageDescriptor(RcpActivator.imageDescriptorFromPlugin(
				RcpActivator.PLUGIN_ID, "icons/network16.png"));
	}

	@Override
	protected void performApply() {
		super.performApply();
		String url = Preference.getUrl();
		String user = Preference.getUser();
		String password = Preference.getPassword();
		NetworkClient client = new NetworkClient(url, user, password);
		testConnection(client);
	}

	private void testConnection(NetworkClient client) {
		try {
			client.connect();
			checkAuthentication(client.getAuthentication());
		} catch (Exception e) {
			Dialog.showError(getShell(), Messages.ILCD_CONNECTION_FAILED_MSG
					+ " ("
					+ e.getMessage() + ")");
		}
	}

	private void checkAuthentication(Authentication authentication) {
		if (!authentication.isAuthenticated())
			Dialog.showError(getShell(),
					Messages.ILCD_AUTHENTICATION_FAILED_MSG);
		else if (!authentication.isReadAllowed()
				|| !authentication.isExportAllowed())
			Dialog.showWarning(getShell(),
					Messages.ILCD_NO_READ_OR_WRITE_ACCESS_MSG);
		else
			Dialog.showInfo(getShell(), Messages.ILCD_CONNECTION_WORKS_MSG);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(RcpActivator.getDefault().getPreferenceStore());
	}

	public static final void open(Shell shell) {
		PreferencesUtil.createPreferenceDialogOn(shell, PreferencePage.ID,
				null, null).open();
	}

}
