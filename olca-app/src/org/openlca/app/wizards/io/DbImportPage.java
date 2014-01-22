package org.openlca.app.wizards.io;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.util.UI;

class DbImportPage extends WizardPage {

	public DbImportPage() {
		super("DbImportPage");
		setTitle(Messages.DatabaseImport);
		setDescription(Messages.DatabaseImportDescription);
	}

	@Override
	public void createControl(Composite parent) {
		Composite body = new Composite(parent, SWT.NONE);
		UI.gridLayout(body, 1);
		Button button = new Button(body, SWT.RADIO);
		button.setText("Existing database");
		button.setSelection(true);


		setControl(body);
	}
}
