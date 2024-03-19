package org.openlca.app.collaboration.dialogs;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.collaboration.navigation.ServerConfigurations;
import org.openlca.app.collaboration.navigation.ServerConfigurations.ServerConfig;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

public class ServerWizard extends Wizard {

	private ServerWizardPage page;

	private ServerWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle("New Collaboration Server");
	}

	public static void open() {
		var wizard = new ServerWizard();
		var dialog = new WizardDialog(UI.shell(), wizard);
		dialog.open();
	}

	@Override
	public void addPages() {
		page = new ServerWizardPage();
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		ServerConfigurations.add(new ServerConfig(page.location.url()));
		Navigator.refresh();
		return true;
	}

	private class ServerWizardPage extends WizardPage {

		private final LocationGroup location = new LocationGroup().onChange(this::updateButtons);

		protected ServerWizardPage() {
			super("server-wizard-page", "New Collaboration Server",
					Icon.COLLABORATION_SERVER_LOGO.descriptor());
			setDescription("Register a new Collaboration Server");
			setPageComplete(false);
		}

		@Override
		public void createControl(Composite parent) {
			var body = UI.composite(parent);
			setControl(body);
			UI.gridLayout(body, 1);
			UI.gridData(body, true, true).widthHint = 500;
			location.render(body, null);
		}

		private void updateButtons() {
			if (Strings.nullOrEmpty(location.url())) {
				setErrorMessage(null);
				setPageComplete(false);
				return;
			}
			if (ServerConfigurations.get().contains(new ServerConfig(location.url()))) {
				setErrorMessage("Server url is already registered");
				setPageComplete(false);
				return;
			}
			setErrorMessage(null);
			setPageComplete(true);
			
		}

	}

}
