package org.openlca.app.collaboration.dialogs;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.collaboration.browse.ServerNavigator;
import org.openlca.app.collaboration.navigation.ServerConfigurations;
import org.openlca.app.collaboration.navigation.ServerConfigurations.ServerConfig;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;

public class ServerWizard extends Wizard {

	private ServerWizardPage page;

	private ServerWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle(M.NewCollaborationServer);
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
		ServerConfigurations.put(new ServerConfig(page.location.url()));
		ServerNavigator.refresh();
		return true;
	}

	private class ServerWizardPage extends WizardPage {

		private LocationGroup location;

		protected ServerWizardPage() {
			super("server-wizard-page", M.NewCollaborationServer,
					Icon.COLLABORATION_SERVER_LOGO.descriptor());
			setDescription(M.RegisterANewCollaborationServer);
			setPageComplete(false);
		}

		@Override
		public void createControl(Composite parent) {
			var body = UI.composite(parent);
			setControl(body);
			UI.gridLayout(body, 1);
			UI.gridData(body, true, true).widthHint = 500;
			location = new LocationGroup(body)
					.onChange(this::updateButtons);
			location.render();
		}

		private void updateButtons() {
			var url = location.url();
			if (Strings.isBlank(url)) {
				setErrorMessage(null);
				setPageComplete(false);
				return;
			}
			if (ServerConfigurations.get().contains(new ServerConfig(url))) {
				setErrorMessage("Server url is already registered");
				setPageComplete(false);
				return;
			}
			setErrorMessage(null);
			setPageComplete(true);
		}

	}

}
