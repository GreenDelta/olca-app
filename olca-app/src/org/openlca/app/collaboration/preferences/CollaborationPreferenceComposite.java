package org.openlca.app.collaboration.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.util.UI;

class CollaborationPreferenceComposite extends Composite {

	private final FormToolkit toolkit;
	private final Button referenceCheckbox;
	private final Button commentCheckbox;
	private final Button fullCommitsCheckbox;

	CollaborationPreferenceComposite(Composite parent, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
		if (toolkit != null) {
			toolkit.adapt(this);
		}
		UI.gridLayout(this, 1, 5, 5);
		fullCommitsCheckbox = createCheckbox(M.AlwaysCommitAllChanges, M.AlwaysCommitAllChangesDescription,
				CollaborationPreference.onlyFullCommits());
		referenceCheckbox = createCheckbox(M.CheckReferencedChanges, M.CheckReferencedChangesDescription,
				CollaborationPreference.checkReferences());
		commentCheckbox = createCheckbox(M.EnableComments, M.EnableCommentsDescription,
				CollaborationPreference.commentsEnabled());
	}

	private Button createCheckbox(String text, String description, boolean selected) {
		var checkbox = UI.button(this, toolkit, text, SWT.CHECK);
		checkbox.setText(text);
		checkbox.setSelection(selected);
		var label = UI.label(this, toolkit, description, SWT.WRAP);
		UI.gridData(label, false, false).widthHint = 800;
		UI.filler(this, toolkit);
		return checkbox;
	}

	public boolean apply() {
		var store = CollaborationPreference.getStore();
		store.setValue(CollaborationPreference.ONLY_FULL_COMMITS, fullCommitsCheckbox.getSelection());
		store.setValue(CollaborationPreference.CHECK_REFERENCES, referenceCheckbox.getSelection());
		store.setValue(CollaborationPreference.DISPLAY_COMMENTS, commentCheckbox.getSelection());
		store.setValue(CollaborationPreference.FIRST_CONFIGURATION, false);
		return true;
	}

}
