package org.openlca.app.wizards.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.devtools.python.Python;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Question;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.derby.DerbyDatabase;
import org.openlca.io.olca.DatabaseImport;
import org.openlca.updates.Update;
import org.openlca.updates.UpdateHelper;
import org.openlca.updates.UpdateMetaInfo;
import org.openlca.updates.VersionState;
import org.openlca.updates.legacy.Upgrades;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

/**
 * Wizards for the import of data from an openLCA database to another openLCA
 * database.
 */
public class DbImportWizard extends Wizard implements IImportWizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private DbImportPage page;

	@Override
	public void init(IWorkbench iWorkbench,
			IStructuredSelection iStructuredSelection) {
		setWindowTitle(M.DatabaseImport);
		setDefaultPageImageDescriptor(Icon.IMPORT_ZIP_WIZARD
				.descriptor());
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		if (Database.get() == null) {
			org.openlca.app.util.Error.showBox(M.NoDatabaseOpened,
					M.DBImportNoTarget);
			return true;
		}
		try {
			Database.getIndexUpdater().beginTransaction();
			DbImportPage.ImportConfig config = page.getConfig();
			ConnectionDispatch connectionDispatch = createConnection(config);
			boolean canRun = canRun(config, connectionDispatch);
			if (!canRun) {
				connectionDispatch.close();
				return false;
			}
			ImportDispatch importDispatch = new ImportDispatch(
					connectionDispatch);
			getContainer().run(true, true, importDispatch);
			return true;
		} catch (Exception e) {
			log.error("database import failed", e);
			return false;
		} finally {
			Database.getIndexUpdater().endTransaction();
			Navigator.refresh();
			Cache.evictAll();
		}
	}

	private boolean canRun(DbImportPage.ImportConfig config,
			ConnectionDispatch connectionDispatch) {
		VersionState state = connectionDispatch.getSourceState();
		if (state == VersionState.UP_TO_DATE)
			return true;
		if (state == null || state == VersionState.ERROR) {
			org.openlca.app.util.Error.showBox(M.ConnectionFailed,
					M.DBImportNoTargetConnectionFailedMessage);
			return false;
		}
		if (state == VersionState.HIGHER_VERSION) {
			org.openlca.app.util.Error
					.showBox(
							M.VersionNewer,
							M.DBImportVersionNewerMessage);
			return false;
		}
		if (config.mode == config.FILE_MODE)
			return true;
		return Question
				.ask(M.UpdateDatabase,
						M.DBImportUpdateDatabaseQuestion);
	}

	private ConnectionDispatch createConnection(DbImportPage.ImportConfig config)
			throws Exception {
		ConnectionDispatch connectionDispatch = new ConnectionDispatch(config);
		PlatformUI.getWorkbench().getProgressService()
				.busyCursorWhile(connectionDispatch);
		return connectionDispatch;
	}

	@Override
	public void addPages() {
		page = new DbImportPage();
		addPage(page);
	}

	private class ImportDispatch implements IRunnableWithProgress {

		private IDatabase sourceDb;
		private VersionState sourceState;
		private ConnectionDispatch connectionDispatch;

		ImportDispatch(ConnectionDispatch connectionDispatch) {
			this.sourceDb = connectionDispatch.getSource();
			this.sourceState = connectionDispatch.getSourceState();
			this.connectionDispatch = connectionDispatch;
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, OperationCanceledException {
			try {
				Database.getIndexUpdater().beginTransaction();
				monitor.beginTask(M.ImportDatabase, IProgressMonitor.UNKNOWN);
				checkAndExecuteUpdates(monitor);
				monitor.subTask(M.ImportData + "...");
				DatabaseImport dbImport = new DatabaseImport(sourceDb, Database.get());
				log.trace("run data import");
				dbImport.run();
				monitor.subTask(M.CloseDatabase);
				connectionDispatch.close();
				monitor.done();
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			} finally {
				Database.getIndexUpdater().endTransaction();
			}
		}

		private void checkAndExecuteUpdates(IProgressMonitor monitor) throws Exception {
			switch (sourceState) {
			case NEEDS_UPGRADE:
				monitor.subTask(M.UpdateDatabase);
				Upgrades.runUpgrades(sourceDb);
			case NEEDS_UPDATE:
				monitor.subTask(M.UpdateDatabase);
				UpdateHelper updates = new UpdateHelper(sourceDb, App.getCalculationContext(), Python.getDir());
				for (UpdateMetaInfo update : updates.getNewAndRequired()) {
					execute(update, updates);
				}
			default:
			}
		}

		private void execute(UpdateMetaInfo metaInfo, UpdateHelper updates) {
			for (String depRefId : metaInfo.dependencies) {
				Update dep = updates.getForRefId(depRefId);
				execute(dep.metaInfo, updates);
			}
			Update update = updates.getForRefId(metaInfo.refId);
			updates.execute(update);
		}

	}

	/**
	 * Creates the initial resources and opens a database connection to the
	 * source database of the import.
	 */
	private class ConnectionDispatch implements IRunnableWithProgress {

		private DbImportPage.ImportConfig config;
		private File tempDbFolder;
		private IDatabase source;

		ConnectionDispatch(DbImportPage.ImportConfig config) {
			this.config = config;
		}

		public IDatabase getSource() {
			return source;
		}

		public VersionState getSourceState() {
			return VersionState.checkVersion(source);
		}

		@Override
		public void run(IProgressMonitor monitor) throws
				InvocationTargetException, InterruptedException {
			log.trace("connect to source database");
			try {
				if (config.mode == config.FILE_MODE)
					source = connectToFolder();
				else
					source = config.databaseConfiguration.createInstance();
			} catch (Exception e) {
				log.error("Failed to connect to source database", e);
				throw new InvocationTargetException(e);
			}
		}

		private IDatabase connectToFolder() {
			File zipFile = config.file;
			File tempDir = new File(System.getProperty("java.io.tmpdir"));
			tempDbFolder = new File(tempDir, UUID.randomUUID().toString());
			tempDbFolder.mkdirs();
			log.trace("unpack zolca file to {}", tempDbFolder);
			ZipUtil.unpack(zipFile, tempDbFolder);
			return new DerbyDatabase(tempDbFolder);
		}

		void close() throws Exception {
			log.trace("close source database");
			source.close();
			if (tempDbFolder != null) {
				log.trace("delete temporary db-folder {}", tempDbFolder);
				FileUtils.deleteDirectory(tempDbFolder);
			}
		}
	}

}
