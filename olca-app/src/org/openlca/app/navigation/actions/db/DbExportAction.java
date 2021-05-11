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
import org.openlca.app.cloud.ui.diff.CompareView;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.db.DerbyConfiguration;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.db.MySQLConfiguration;
import org.openlca.app.db.MySQLDatabaseExport;
import org.openlca.app.editors.Editors;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Popup;
import org.openlca.cloud.api.RepositoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

public class DbExportAction extends Action implements INavigationAction {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private DatabaseElement element;

	public DbExportAction() {
		setText(M.BackupDatabase);
		setImageDescriptor(Icon.DATABASE_EXPORT.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement))
			return false;
		this.element = (DatabaseElement) first;
		return true;
	}

	@Override
	public void run() {
		var config = element == null || element.getContent() == null
				? Database.getActiveConfiguration()
				: element.getContent();
		if (config == null)
			return;
		var file = FileChooser.forSavingFile(
				M.Export, config.getName() + ".zolca");
		if (file == null)
			return;
		run(config, file, Database.isActive(config));
	}

	private void run(IDatabaseConfiguration config, final File zip, final boolean active) {
		if (zip.exists()) {
			log.trace("delete existing file {}", zip);
			boolean deleted = zip.delete();
			if (!deleted) {
				MsgBox.error(M.CouldNotOverwriteFile + ": " + zip.getName());
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
			if (config instanceof DerbyConfiguration) {
				File folder = DatabaseDir.getRootFolder(config.getName());
				ZipEntrySource[] toPack = collectFileSources(folder);
				ZipUtil.pack(toPack, zip);
			} else if (config instanceof MySQLConfiguration) {
				MySQLDatabaseExport export = new MySQLDatabaseExport((MySQLConfiguration) config, zip);
				export.run();
			}
		} catch (Exception e) {
			ErrorReporter.on("Export of database " + config.getName() + " failed", e);
		}
	}

	private ZipEntrySource[] collectFileSources(File folder) throws IOException {
		List<ZipEntrySource> fileSources = new ArrayList<>();
		Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				String relativePath = folder.toPath().relativize(file).toString();
				if (exclude(relativePath))
					return FileVisitResult.CONTINUE;
				fileSources.add(new FileSource(relativePath.replace("\\", "/"), file.toFile()));
				return FileVisitResult.CONTINUE;
			}
		});
		return fileSources.toArray(new ZipEntrySource[0]);
	}

	private boolean exclude(String relativePath) {
		return relativePath.startsWith(
				DatabaseDir.FILE_STORAGE + File.separator + RepositoryConfig.DIR);
	}

	private void updateUI(File zip, boolean active) {
		if (active) {
			Navigator.refresh();
			CompareView.clear();
		}
		HistoryView.refresh();
		Popup.info(M.ExportDone, M.DatabaseWasExportedToFile + ": " + zip.getName());
	}
}
