package org.openlca.app.navigation.actions.db;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.db.DerbyConfiguration;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.db.ILocalDatabaseConfiguration;
import org.openlca.app.db.IRemoteDatabaseConfiguration;
import org.openlca.app.db.MySQLConfiguration;
import org.openlca.app.db.PostgresConfiguration;
import org.openlca.app.db.RemoteDatabaseExport;
import org.openlca.app.editors.Editors;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Info;
import org.openlca.cloud.api.RepositoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

public class DbExportAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(getClass());
	private DatabaseElement element;

	public DbExportAction() {
		setText(M.BackupDatabase);
		setImageDescriptor(Icon.DATABASE_EXPORT.descriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement e = (DatabaseElement) element;
		this.element = e;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		IDatabaseConfiguration config = null;
		if (element == null || element.getContent() == null) {
			config = Database.getActiveConfiguration();
		} else {
			config = element.getContent();
		}
		if (config == null)
			return;
		File file = FileChooser.forExport("*.zolca", config.getName() + ".zolca");
		if (file == null)
			return;
		run(config, file, Database.isActive(config));
	}

	private void run(IDatabaseConfiguration config, final File zip, final boolean active) {
		if (zip.exists()) {
			log.trace("delete existing file {}", zip);
			boolean deleted = zip.delete();
			if (!deleted) {
				org.openlca.app.util.Error.showBox(M.CouldNotOverwriteFile + ": " + zip.getName());
				return;
			}
		}
		if (active)
			if (!Editors.closeAll())
				return;
		log.trace("run database export to file {}", zip);
		App.runWithProgress(M.ExportDatabase, () -> realExport(config, zip, active));
		updateUI(zip, active);
	}

	private void realExport(IDatabaseConfiguration config, File zip, boolean active) {
		try {
			if (active)
				Database.close();
			if (config instanceof ILocalDatabaseConfiguration) {
				File folder = DatabaseDir.getRootFolder(config.getName());
				ZipEntrySource[] toPack = collectFileSources(folder);
				ZipUtil.pack(toPack, zip);
			} else if (config instanceof IRemoteDatabaseConfiguration) {
				RemoteDatabaseExport export = new RemoteDatabaseExport(config, zip);
				export.run();
			}
		} catch (Exception e) {
			log.error("Export failed " + zip, e);
		}
	}

	private ZipEntrySource[] collectFileSources(File folder) throws IOException {
		List<ZipEntrySource> fileSources = new ArrayList<>();
		Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String relativePath = folder.toPath().relativize(file).toString();
				if (exclude(relativePath))
					return FileVisitResult.CONTINUE;
				fileSources.add(new FileSource(relativePath.replace("\\", "/"), file.toFile()));
				return FileVisitResult.CONTINUE;
			}
		});
		return fileSources.toArray(new ZipEntrySource[fileSources.size()]);
	}

	private boolean exclude(String relativePath) {
		if (relativePath.equals(DatabaseDir.FILE_STORAGE + File.separator + RepositoryConfig.PROPERTIES_FILE))
			return true;
		if (relativePath.startsWith(DatabaseDir.FILE_STORAGE + File.separator + RepositoryConfig.INDEX_DIR))
			return true;
		return false;
	}

	private void updateUI(final File zip, final boolean active) {
		if (active)
			Navigator.refresh();
		HistoryView.refresh();
		Info.popup(M.ExportDone, M.DatabaseWasExportedToFile + ": " + zip.getName());
	}
}
