package org.openlca.app.rcp.update;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.UI;

public class ShowUpdaterPreferencesAction extends Action {

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.CONNECT_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.UpdatePreferences;
	}

	@Override
	public void run() {
		PreferenceDialog preferenceDialog = PreferencesUtil
				.createPreferenceDialogOn(
						UI.shell(),
						"org.openlca.core.application.update.UpdatePreferencePage",
						null, null);
		preferenceDialog.open();
	}
}
