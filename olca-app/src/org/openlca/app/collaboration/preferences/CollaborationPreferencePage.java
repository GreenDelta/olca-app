package org.openlca.app.collaboration.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class CollaborationPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID = "preferencepages.collaboration";
	private CollaborationPreferenceComposite preferences;

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(CollaborationPreference.getStore());
	}

	@Override
	protected Control createContents(Composite parent) {
		preferences = new CollaborationPreferenceComposite(parent, null);
		return preferences;
	}

	@Override
	public boolean performOk() {
		preferences.apply();
		return true;
	}
}
