package org.openlca.app.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;

public class MemoryError {

	public static void show() {
		App.runInUI("Memory error in calculation...", () -> new Dialog().open());
	}

	private static class Dialog extends FormDialog {

		public Dialog() {
			super(UI.shell());
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
			return new Point(450, 350);
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(M.OutOfMemory);
		}

		@Override
		protected void createFormContent(IManagedForm form) {
			var tk = form.getToolkit();
			var comp = UI.dialogBody(form.getForm(), tk);
			form.getForm().setText(M.OutOfMemory);
			UI.gridLayout(comp, 1);
			var label = tk.createLabel(
					comp, M.CouldNotAllocateMemoryError, SWT.WRAP);
			UI.fillHorizontal(label);
			var link = UI.hyperLink(comp, tk, "Open preference dialog");
			Controls.onClick(link, $ -> openPreferences());
		}

		private void openPreferences() {
			close();
			var dialog = PreferencesUtil.createPreferenceDialogOn(
					null, "preferencepages.config", null, null);
			dialog.open();
		}
	}
}
