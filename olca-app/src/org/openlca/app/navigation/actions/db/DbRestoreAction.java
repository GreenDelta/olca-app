package org.openlca.app.navigation.actions.db;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.Config;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseDirElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.NavigationRoot;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.util.Strings;
import org.zeroturnaround.zip.ZipUtil;

public class DbRestoreAction extends Action implements INavigationAction {

	private String naviFolder;

	public DbRestoreAction() {
		setText(M.RestoreDatabase);
		setImageDescriptor(Icon.DATABASE_IMPORT.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		naviFolder = "";
		if (selection.isEmpty())
			return true;
		var first = selection.get(0);
		if (first instanceof DatabaseDirElement dir) {
			naviFolder = String.join("/", dir.path());
			return true;
		}
		return first instanceof DatabaseElement
				|| first instanceof NavigationRoot;
	}

	@Override
	public void run() {
		var zolca = FileChooser.open("*.zolca");
		if (zolca == null || !zolca.exists())
			return;
		run(zolca, naviFolder);
	}

	public static void run(File zolca) {
		run(zolca, "");
	}

	private static void run(File zolca, String naviFolder) {
		if (zolca == null || !zolca.exists())
			return;
		try {
			var dbFolder = new File(
					Workspace.root(), Config.DATABASE_FOLDER_NAME);
			Files.createDirectories(dbFolder.toPath());
			var dbName = getDatabaseName(zolca, dbFolder);
			realImport(dbFolder, dbName, zolca, naviFolder);
		} catch (Exception e) {
			ErrorReporter.on("Failed to restore database" +
					" from file: " + zolca.getName(), e);
		}

	}

	private static String getDatabaseName(File zolca, File dbFolder) {
		var proposal = zolca.getName();
		if (proposal.toLowerCase(Locale.US).endsWith(".zolca")) {
			proposal = proposal.substring(0, proposal.length() - 6);
		}
		var existing = dbFolder.list();
		var names = existing == null
				? Collections.emptySet()
				: Arrays.stream(existing)
				.map(String::toLowerCase)
				.collect(Collectors.toSet());
		String name = proposal;
		int count = 0;
		while (names.contains(name.toLowerCase())) {
			count++;
			name = proposal + " (" + count + ")";
		}
		return name;
	}

	private static void realImport(
			File dbFolder, String dbName, File zip, String naviFolder) {
		var err = new AtomicBoolean(false);
		App.run(M.ImportDatabase, () -> {
			try {
				var folder = new File(dbFolder, dbName);
				Files.createDirectories(folder.toPath());
				ZipUtil.unpack(zip, folder);
			} catch (Exception e) {
				err.set(true);
			}
		}, () -> {
			if (err.get()) {
				MsgBox.error("Failed to create database");
				return;
			}

			var conf = new DerbyConfig();
			conf.name(dbName);
			if (Strings.notEmpty(naviFolder)) {
				conf.setCategory(naviFolder);
			}
			Database.register(conf);
			Navigator.refresh();
		});
	}
}
