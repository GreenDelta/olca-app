package org.openlca.app.collaboration.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openlca.app.M;
import org.openlca.app.util.UI;

public class CollaborationPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID = "preferencepages.collaboration";
	private Button referenceCheckbox;
	private Button commentCheckbox;
	private Button fullCommitCheckbox;

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(CollaborationPreference.getStore());
	}

	@Override
	protected Control createContents(Composite parent) {
		var body = UI.composite(parent);
		UI.gridLayout(body, 1);
		var general = UI.composite(body);
		UI.gridLayout(general, 2, 0, 0);
		referenceCheckbox = createCheckbox(general, M.CheckReferencedChanges, CollaborationPreference.checkReferences());
		commentCheckbox = createCheckbox(general, M.EnableComments, CollaborationPreference.checkReferences());
		fullCommitCheckbox = createCheckbox(general, M.AlwaysCommitAllChanges, CollaborationPreference.onlyFullCommits());
		return body;
	}

	private Button createCheckbox(Composite parent, String text, boolean selected) {
		var label = new Label(parent, SWT.NONE);
		label.setText(text);
		var gd = UI.gridData(label, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;

		var checkBox = new Button(parent, SWT.CHECK);
		UI.gridData(checkBox, true, false).horizontalIndent = 5;
		checkBox.setSelection(selected);		
		return checkBox;
	}
	
	@Override
	public boolean performOk() {
		var store = CollaborationPreference.getStore();
		store.setValue(CollaborationPreference.CHECK_REFERENCES, referenceCheckbox.getSelection());
		store.setValue(CollaborationPreference.DISPLAY_COMMENTS, commentCheckbox.getSelection());
		store.setValue(CollaborationPreference.ONLY_FULL_COMMIT, fullCommitCheckbox.getSelection());
		return true;
	}
}
