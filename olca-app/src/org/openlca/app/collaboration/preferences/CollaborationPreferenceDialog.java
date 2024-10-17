package org.openlca.app.collaboration.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.util.UI;

public class CollaborationPreferenceDialog extends FormDialog {

	private CollaborationPreferenceComposite preferences;

	public CollaborationPreferenceDialog() {
		super(UI.shell());
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var toolkit = form.getToolkit();
		var body = UI.body(form.getForm(), toolkit);
		var description = UI.label(body, toolkit, M.FirstConfigurationDescription, SWT.WRAP);
		UI.gridData(description, false, false).widthHint = 800;
		var separator = UI.label(body, toolkit, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		preferences = new CollaborationPreferenceComposite(body, toolkit);
	}
	
	@Override
	protected void okPressed() {
		preferences.apply();
		super.okPressed();
	}

}
