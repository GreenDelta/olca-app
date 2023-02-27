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
	private Button restrictionsCheckBox;
	private Button referenceCheckBox;
	private Button commentCheckBox;

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(CollaborationPreference.getStore());
	}

	@Override
	protected Control createContents(Composite parent) {
		var body = new Composite(parent, SWT.NONE);
		UI.gridLayout(body, 1);
		var general = new Composite(body, SWT.NONE);
		UI.gridLayout(general, 2, 0, 0);
		createRestrictionsCheckBox(general);
		createReferenceCheckBox(general);
		createCommentCheckBox(general);
		return body;
	}

	private void createRestrictionsCheckBox(Composite parent) {
		var label = new Label(parent, SWT.NONE);
		label.setText("Check restrictions before commit");
		var gd = UI.gridData(label, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;

		restrictionsCheckBox = new Button(parent, SWT.CHECK);
		UI.gridData(restrictionsCheckBox, true, false).horizontalIndent = 5;
		restrictionsCheckBox.setSelection(CollaborationPreference.checkRestrictions());
	}

	private void createReferenceCheckBox(Composite parent) {
		var label = new Label(parent, SWT.NONE);
		label.setText("Check referenced changes");
		var gd = UI.gridData(label, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;

		referenceCheckBox = new Button(parent, SWT.CHECK);
		UI.gridData(referenceCheckBox, true, false).horizontalIndent = 5;
		referenceCheckBox.setSelection(CollaborationPreference.checkReferences());
	}

	private void createCommentCheckBox(Composite parent) {
		var label = new Label(parent, SWT.NONE);
		label.setText(M.EnableComments);
		var gd = UI.gridData(label, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;

		commentCheckBox = new Button(parent, SWT.CHECK);
		UI.gridData(commentCheckBox, true, false).horizontalIndent = 5;
		commentCheckBox.setSelection(CollaborationPreference.commentsEnabled());
	}

	@Override
	public boolean performOk() {
		var store = CollaborationPreference.getStore();
		store.setValue(CollaborationPreference.CHECK_RESTRICTIONS, restrictionsCheckBox.getSelection());
		store.setValue(CollaborationPreference.CHECK_REFERENCES, referenceCheckBox.getSelection());
		store.setValue(CollaborationPreference.DISPLAY_COMMENTS, commentCheckBox.getSelection());
		return true;
	}
}
