package org.openlca.app.editors.systems;

import org.openlca.app.M;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.HyperlinkListener;
import org.openlca.app.util.UI;

class OOMError {

	public static void show() {
		OOMErrorDialog dialog = new OOMErrorDialog();
		dialog.open();
	}

	private static class OOMErrorDialog extends FormDialog {

		public OOMErrorDialog() {
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
			return new Point(400, 280);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			String message = M.CouldNotAllocateMemoryError;
			FormToolkit toolkit = mform.getToolkit();
			mform.getForm().setText(M.OutOfMemory);
			Composite comp = UI.formBody(mform.getForm(), mform.getToolkit());
			UI.gridLayout(comp, 1);
			Label label = toolkit.createLabel(comp, message, SWT.WRAP);
			UI.gridData(label, true, false);
			UI.formLink(comp, toolkit, "Open preference dialog")
					.addHyperlinkListener(
							new HyperlinkListener(this::openPreferences));
		}

		private void openPreferences() {
			close();
			PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
					null, "preferencepages.config", null, null);
			dialog.open();
		}

	}

}
