package org.openlca.app.rcp.browser;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.progress.UIJob;
import org.openlca.app.Preferences;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.UI;

class VsCpp10Message {

	static void checkAndShow() {
		boolean hide = Preferences.getStore().getBoolean("hide.vscpp10message");
		if (hide)
			return;
		new UIJob("No VC++ Runtime Installed") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				new Dialog(UI.shell()).open();
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	private static class Dialog extends FormDialog {

		public Dialog(Shell shell) {
			super(shell);
		}

		@Override
		protected Point getInitialLocation(Point initialSize) {
			int y = (getParentShell().getSize().y / 2 - initialSize.y / 2);
			int x = (getParentShell().getSize().x / 2 - initialSize.x / 2);
			if (y < 0)
				y = 0;
			return new Point(x, y);
		}

		@Override
		protected Point getInitialSize() {
			return new Point(400, 280);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			super.createFormContent(mform);
			getShell().setText("Enable modern browser support");
			UI.formHeader(mform, "Enable modern browser support");
			Composite body = UI.formBody(mform.getForm(), mform.getToolkit());
			UI.gridLayout(body, 1);
			createLink(mform, body);
			Button check = mform.getToolkit().createButton(body,
					"Do not show this message again", SWT.CHECK);
			Controls.onSelect(check, (e) -> {
				Preferences.getStore()
						.setValue("hide.vscpp10message", check.getSelection());
			});
		}

		private void createLink(IManagedForm mform, Composite body) {
			Link link = new Link(body, SWT.WRAP);
			mform.getToolkit().adapt(link, true, true);
			String message = getMessage();
			link.setText(message);
			UI.gridData(link, true, false);
			Controls.onSelect(link, (e) -> Desktop.browse(e.text));
		}

		private String getMessage() {
			String message = "openLCA contains a browser engine for the display "
					+ "of modern HTML pages. It requires the "
					+ "<a href=\"http://www.microsoft.com/de-de/download/details.aspx?id=14632\">"
					+ "Microsoft Visual C++ Runtime v10</a> to be installed on "
					+ "Windows 64bit which seems to be not the case on your "
					+ "system. We recommend to install this runtime if you want "
					+ "to use the advanced visualisation features of openLCA.";
			return message;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID,
					IDialogConstants.OK_LABEL, true);
		}
	}
}
