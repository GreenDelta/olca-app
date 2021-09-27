package org.openlca.app.editors.results.openepd;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

public class ImportWizard extends Wizard implements IImportWizard {

	private final Credentials credentials = Credentials.init();

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import an EPD result");
	}

	@Override
	public boolean performFinish() {
		credentials.save();
		return true;
	}

	@Override
	public void addPages() {
		addPage(new Page());
	}

	private class Page extends WizardPage {

		Page() {
			super("Search");
			setTitle("Search for EPD results");
			setDescription("Login with your EC3 account and search for an EPD");
		}

		@Override
		public void createControl(Composite parent) {
			var root = new Composite(parent, SWT.NONE);
			setControl(root);
			UI.gridLayout(root, 1, 0, 5);
			root.setLayout(new GridLayout(1, false));

			var comp = new Composite(root, SWT.NONE);
			UI.gridData(comp, true, false);
			UI.gridLayout(comp, 2);

			var urlText = UI.formText(comp, "URL");
			urlText.setText(Strings.orEmpty(credentials.url));
			urlText.addModifyListener(
				$ -> credentials.url = urlText.getText());

			var userText = UI.formText(comp, "User");
			userText.setText(Strings.orEmpty(credentials.user));
			userText.addModifyListener(
				$ -> credentials.user = userText.getText());

			var pwText = UI.formText(comp, "Password", SWT.PASSWORD);
			pwText.setText(Strings.orEmpty(credentials.password));
			pwText.addModifyListener(
				$ -> credentials.password = pwText.getText());

		}
	}
}
