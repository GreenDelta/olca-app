package org.openlca.app.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openlca.app.M;

public class FeatureFlagPage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public FeatureFlagPage() {
		super(FieldEditorPreferencePage.GRID);
		setDescription(M.ExperimentalFeaturesWarn);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Preferences.getStore());
	}

	@Override
	protected void createFieldEditors() {
		for (FeatureFlag flag : FeatureFlag.values()) {
			BooleanFieldEditor field = new BooleanFieldEditor(flag.name(),
					flag.getDescription(), getFieldEditorParent());
			addField(field);
		}
	}

}
