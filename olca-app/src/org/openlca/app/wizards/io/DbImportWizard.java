package org.openlca.app.wizards.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.core.database.Derby;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.upgrades.Upgrades;
import org.openlca.core.database.upgrades.VersionState;
import org.openlca.io.olca.DatabaseImport;
import org.openlca.util.Dirs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

/**
 * Wizards for the import of data from an openLCA database to another openLCA
 * database.
 */
public class DbImportWizard extends Wizard implements IImportWizard {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private DbImportPage page;

	private File initialFile;

	public static void of(File file) {
		Wizards.forImport(
				"wizard.import.db",
				(DbImportWizard w) -> w.initialFile = file);
	}

	public DbImportWizard() {
		setWindowTitle(M.DatabaseImport);
		setDefaultPageImageDescriptor(Icon.IMPORT_ZIP_WIZARD.descriptor());
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		page = initialFile != null
				? new DbImportPage(initialFile)
				: new DbImportPage();
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		if (Database.get() == null) {
			MsgBox.error(M.NoDatabaseOpened, M.DBImportNoTarget);
			return true;
		}
		try {
			var config = page.getConfig();
			var connectionDispatch = createConnection(config);
			boolean canRun = canRun(config, connectionDispatch);
			if (!canRun) {
				connectionDispatch.close();
				return false;
			}
			ImportDispatch importDispatch = new ImportDispatch(connectionDispatch);
			getContainer().run(true, true, importDispatch);
			return true;
		} catch (Exception e) {
			ErrorReporter.on("Database import failed", e);
			return false;
		} finally {
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
			MsgBox.error(M.ConnectionFailed,
					M.DBImportNoTargetConnectionFailedMessage);
			return false;
		}
		if (state == VersionState.HIGHER_VERSION) {
			MsgBox.error(M.VersionNewer, M.DBImportVersionNewerMessage);
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

	private class ImportDispatch implements IRunnableWithProgress {

		private final IDatabase sourceDb;
		private final VersionState sourceState;
		private final ConnectionDispatch connectionDispatch;

		ImportDispatch(ConnectionDispatch connectionDispatch) {
			this.sourceDb = connectionDispatch.getSource();
			this.sourceState = connectionDispatch.getSourceState();
			this.connectionDispatch = connectionDispatch;
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, OperationCanceledException {
			try {
				Database.getWorkspaceIdUpdater().beginTransaction();
				monitor.beginTask(M.ImportDatabase, IProgressMonitor.UNKNOWN);
				if (sourceState == VersionState.NEEDS_UPGRADE) {
					monitor.subTask(M.UpdateDatabase);
					Upgrades.on(sourceDb);
				}
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
				Database.getWorkspaceIdUpdater().endTransaction();
			}
		}
	}

	/**
	 * Creates the initial resources and opens a database connection to the
	 * source database of the import.
	 */
	private class ConnectionDispatch implements IRunnableWithProgress {

		private final DbImportPage.ImportConfig config;
		private File tempDbFolder;
		private IDatabase source;

		ConnectionDispatch(DbImportPage.ImportConfig config) {
			this.config = config;
		}

		public IDatabase getSource() {
			return source;
		}

		public VersionState getSourceState() {
			return VersionState.get(source);
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException {
			log.trace("connect to source database");
			try {
				source = config.mode == config.FILE_MODE
					? connectToFolder()
					: config.databaseConfiguration.connect(Workspace.dbDir());
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
			return new Derby(tempDbFolder);
		}

		void close() throws Exception {
			log.trace("close source database");
			source.close();
			if (tempDbFolder != null) {
				log.trace("delete temporary db-folder {}", tempDbFolder);
				Dirs.delete(tempDbFolder);
			}
		}
	}
}
