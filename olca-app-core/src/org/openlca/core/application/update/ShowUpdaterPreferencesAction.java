package org.openlca.core.application.update;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;

public class ShowUpdaterPreferencesAction extends Action {

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.CONNECT_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.update_preferences;
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
