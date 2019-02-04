package org.openlca.app.devtools.ipc;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.Error;
import org.openlca.app.util.Info;
import org.openlca.app.util.UI;

public class IpcDialog extends FormDialog {

	private Text portText;

	public static int show() {
		if (Database.get() == null) {
			Info.showBox(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return Window.CANCEL;
		}
		return new IpcDialog().open();
	}

	public IpcDialog() {
		super(UI.shell());
		setBlockOnOpen(true);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("#Start an IPC Server");
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		FormToolkit tk = mForm.getToolkit();
		Composite body = UI.formBody(mForm.getForm(), tk);
		Composite comp = UI.formComposite(body, tk);
		UI.gridData(comp, true, false);
		portText = UI.formText(comp, tk, M.Port);
		portText.setText("8080");
	}

	@Override
	protected void okPressed() {
		String portStr = portText.getText();
		try {
			int port = Integer.parseInt(portStr);
			Database.startIpcOn(port);
			// TODO: need a UI for managing the server
			super.okPressed();
		} catch (Exception e) {
			Error.showBox("#Failed to start IPC server",
					e.getMessage());
		}
	}
}
