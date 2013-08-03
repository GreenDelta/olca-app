package org.openlca.app.update;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openlca.app.Messages;

/**
 * The preference page for the auto-update configuration.
 */
public class UpdatePreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	boolean updatingWasEnabledOnPageCreation = false;

	@Override
	protected void createFieldEditors() {
		updatingWasEnabledOnPageCreation = UpdatePreference.isUpdateEnabled();

		Composite parent = getFieldEditorParent();

		String[][] updateRythm = new String[][] {
				{ Messages.NEVER_CHECK, UpdatePreference.UPDATE_RYTHM_NEVER },
				{ Messages.HOURLY, UpdatePreference.UPDATE_RYTHM_HOURLY },
				{ Messages.DAILY, UpdatePreference.UPDATE_RYTHM_DAILY },
				{ Messages.WEEKLY, UpdatePreference.UPDATE_RYTHM_WEEKLY },
				{ Messages.MONTHLY, UpdatePreference.UPDATE_RYTHM_MONTHLY }, };
		RadioGroupFieldEditor rythmRadios = new RadioGroupFieldEditor(
				UpdatePreference.UPDATE_RYTHM_SECS, Messages.check_for_updates,
				1, updateRythm, parent, true);
		addField(rythmRadios);

	}

	@Override
	public boolean performOk() {
		boolean b = super.performOk();
		if (b) {
			// run an update check if updating was enabled just now:
			if (!updatingWasEnabledOnPageCreation) {
				if (UpdatePreference.isUpdateEnabled()) {
					new UpdateCheckAndPrepareJob().schedule();
				}
			}
		}
		return b;
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(UpdatePreference.getStore());
	}

}
