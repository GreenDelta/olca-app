package org.openlca.app.wizards.io;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.util.UI;

class NoDatabaseErrorPage extends WizardPage  {

	NoDatabaseErrorPage() {
		super("NoDatabasePage");
		setTitle(M.NoDatabaseOpened);
		setDescription(M.NeedOpenDatabase);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		var body = UI.composite(parent);
		UI.gridLayout(body, 1);
		setControl(body);
	}
}
