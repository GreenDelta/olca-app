package org.openlca.app.db;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.db.DatabaseWizardPage.DerbyPageData;
import org.openlca.app.db.DatabaseWizardPage.MySQLPageData;
import org.openlca.app.db.DatabaseWizardPage.PageData;
import org.openlca.app.events.DatabaseCreatedEvent;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.UI;
import org.openlca.core.database.DatabaseContent;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.derby.DerbyDatabase;
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
		page = new DatabaseWizardPage();
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		try {
			data = page.getPageData();
			getContainer().run(true, false, this);
			Navigator.refresh();
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
		IDatabaseConfiguration configuration = null;
		if (data instanceof DerbyPageData) {
			DerbyConfiguration config = new DerbyConfiguration();
			config.setFolder(new File(App.getWorkspace(), "databases"));
			config.setName(data.databaseName);
			Database.register(config);
			configuration = config;
		} else if (data instanceof MySQLPageData) {
			MySQLPageData mysqlData = (MySQLPageData) data;
			MySQLConfiguration config = new MySQLConfiguration();
			config.setHost(mysqlData.host);
			config.setPort(mysqlData.port);
			config.setUser(mysqlData.user);
			config.setPassword(mysqlData.password);
			config.setName(mysqlData.databaseName);
			Database.register(config);
			configuration = config;
		}
		try {
			Database.close();
			IDatabase database = Database.activate(configuration);
			fillContent(database);
		} catch (Exception e) {
			log.error("Create database failed", e);
		}
		Navigator.refresh();
		App.getEventBus().post(new DatabaseCreatedEvent(Database.get()));
		monitor.done();
	}

	private void fillContent(IDatabase database) {
		if (!(database instanceof DerbyDatabase))
			return;
		DerbyDatabase db = (DerbyDatabase) database;
		DerbyPageData dData = (DerbyPageData) data;
		DatabaseContent content = dData.contentType;
		if (content != DatabaseContent.EMPTY)
			db.fill(content);
	}
}
