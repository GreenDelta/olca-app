package org.openlca.core.application.db;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.core.application.App;
import org.openlca.core.application.Messages;
import org.openlca.core.application.db.DatabaseWizardPage.PageData;
import org.openlca.core.application.views.navigator.Navigator;
import org.openlca.core.application.views.search.SearchView;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The wizard for database creation.
 */
public class DatabaseWizard extends Wizard implements IRunnableWithProgress {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private DatabaseWizardPage page;
	private PageData data;

	public DatabaseWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle(Messages.NewDatabase);
	}

	@Override
	public void addPages() {
		page = new DatabaseWizardPage(getExistingNames());
		addPage(page);
	}

	private String[] getExistingNames() {
		return null;
		// try {
		// List<DatabaseDescriptor> databases = dataProvider
		// .getDatabaseDescriptors();
		// String[] names = new String[databases.size()];
		// for (int i = 0; i < databases.size(); i++)
		// names[i] = databases.get(i).getName();
		// return names;
		// } catch (Exception e) {
		// log.error("Could not get databases", e);
		// return new String[0];
		// }
	}

	@Override
	public boolean performFinish() {
		try {
			data = page.getPageData();
			getContainer().run(true, false, this);
			Navigator.refresh(2);
			SearchView.refresh();
			return true;
		} catch (Exception e) {
			log.error("Database creation failed", e);
			return false;
		}
	}

	public static void open() {
		DatabaseWizard wizard = new DatabaseWizard();
		WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
		dialog.open();
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask(Messages.NewDatabase_Create, IProgressMonitor.UNKNOWN);
		try {
			DerbyConfiguration config = new DerbyConfiguration();
			config.setFolder(new File(App.getWorkspace(), "databases"));
			config.setName(data.databaseName);
			Database.register(config);
			Navigator.refresh();
			// App.getEventBus().post(new DatabaseCreatedEvent(database));
			monitor.done();
		} catch (final Exception e1) {
			log.error("Create database failed", e1);
		}
	}
}
