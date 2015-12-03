package org.openlca.app.cloud.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class CloudPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(CloudPreference.getStore());
	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
		BooleanFieldEditor checkAgainstLibraries = new BooleanFieldEditor(
				CloudPreference.CHECK_AGAINST_LIBRARIES,
				"#Check against libraries", parent);
		addField(checkAgainstLibraries);
	}

}
