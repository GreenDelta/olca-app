package org.openlca.app.rcp;

import org.openlca.app.M;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.Preferences;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;

class MissingLibraryMessage {

	static void checkAndShow() {
		boolean hide = Preferences.getStore().getBoolean("hide.missing_library_message");
		if (hide)
			return;
		new Dialog(UI.shell()).open();
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
			return new Point(400, 300);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			super.createFormContent(mform);
			getShell().setText(M.EnableJblasLibrary);
			UI.formHeader(mform, M.EnableJblasLibrary);
			Composite body = UI.formBody(mform.getForm(), mform.getToolkit());
			UI.gridLayout(body, 1);
			createMessage(mform, body);
			Button check = mform.getToolkit().createButton(body, M.DoNotShowThisMessageAgain, SWT.CHECK);
			Controls.onSelect(check, (e) -> {
				Preferences.getStore().setValue("hide.vscpp10message", check.getSelection());
			});
		}

		private void createMessage(IManagedForm mform, Composite body) {
			Label label = new Label(body, SWT.WRAP);
			mform.getToolkit().adapt(label, true, true);
			label.setText(M.LibgfortanInfo);
			UI.gridData(label, true, false);
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		}
	}
}
