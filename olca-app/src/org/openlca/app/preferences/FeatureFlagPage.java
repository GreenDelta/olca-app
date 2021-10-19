package org.openlca.app.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class FeatureFlagPage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public FeatureFlagPage() {
		super(FieldEditorPreferencePage.GRID);
		setDescription("Warning: Experimental features are less tested"
				+ " and may are removed in future versions of openLCA.");
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
