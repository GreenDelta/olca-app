package org.openlca.app.logging;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * The preference page for the logger configuration.
 */
public class LoggerConfigPage extends FieldEditorPreferencePage implements
	IWorkbenchPreferencePage {

	@Override
	protected void createFieldEditors() {
		var parent = getFieldEditorParent();
		var logConsole = new BooleanFieldEditor(
			LoggerPreference.LOG_CONSOLE,
			"Show log console",
			getFieldEditorParent());
		addField(logConsole);
		String[][] logLevel = new String[][]{
			{"All", LoggerPreference.LEVEL_ALL},
			{"Information", LoggerPreference.LEVEL_INFO},
			{"Warnings", LoggerPreference.LEVEL_WARN},
			{"Errors", LoggerPreference.LEVEL_ERROR}};
		var logRadios = new RadioGroupFieldEditor(
			LoggerPreference.LOG_LEVEL, "Log-Level:", 1, logLevel, parent, true);
		addField(logRadios);
	}

	@Override
	public boolean performOk() {
		boolean b = super.performOk();
		if (b) {
			var level = LoggerPreference.getLogLevel();
			LoggerConfig.setLevel(level);
			if (LoggerPreference.getShowConsole()) {
				Console.show();
			} else {
				Console.dispose();
			}
		}
		return b;
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(LoggerPreference.store());
	}
}
