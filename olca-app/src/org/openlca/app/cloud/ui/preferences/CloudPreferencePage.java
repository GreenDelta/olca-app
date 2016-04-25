package org.openlca.app.cloud.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openlca.app.M;

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
				M.CheckAgainstLibraries, parent);
		new Label(parent, SWT.NONE);
		addField(checkAgainstLibraries);
		StringFieldEditor defaultHost = new StringFieldEditor(
				CloudPreference.DEFAULT_HOST,
				"#Default server url", parent);
		addField(defaultHost);
		StringFieldEditor defaultUser = new StringFieldEditor(
				CloudPreference.DEFAULT_USER,
				"#Default user", parent);
		addField(defaultUser);
		StringFieldEditor defaultPass = new StringFieldEditor(
				CloudPreference.DEFAULT_PASS,
				"#Default password", parent);
		defaultPass.getTextControl(parent).setEchoChar('*');
		addField(defaultPass);
	}

}
