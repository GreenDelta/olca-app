package org.openlca.app.navigation.actions.db;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.Config;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.db.DerbyConfiguration;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.rcp.images.Icon;
import org.zeroturnaround.zip.ZipUtil;

public class DbImportAction extends Action implements INavigationAction {

	public DbImportAction() {
		setText(M.RestoreDatabase);
		setImageDescriptor(Icon.DATABASE_IMPORT.descriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		return false;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		File file = FileChooser.forImport("*.zolca");
		if (file == null || !file.exists())
			return;
		File dbFolder = new File(App.getWorkspace(),
				Config.DATABASE_FOLDER_NAME);
		if (!dbFolder.exists())
			dbFolder.mkdirs();
		String dbName = getDatabaseName(file, dbFolder);
		realImport(dbFolder, dbName, file);
	}

	private String getDatabaseName(File file, File dbFolder) {
		String proposal = file.getName()
				.replace(".zolca", "")
				.replaceAll("\\W+", "_")
				.toLowerCase();
		List<String> names = new ArrayList<>();
		for (String existing : dbFolder.list())
			names.add(existing.toLowerCase());
		String name = proposal;
		int count = 0;
		while (names.contains(name.toLowerCase())) {
			count++;
			name = proposal + "_" + count;
		}
		return name;
	}

	private void realImport(File dbFolder, String dbName, File zip) {
		App.run(M.ImportDatabase, () -> {
			File folder = new File(dbFolder, dbName);
			folder.mkdirs();
			ZipUtil.unpack(zip, folder);
		}, () -> {
			DerbyConfiguration conf = new DerbyConfiguration();
			conf.setName(dbName);
			Database.register(conf);
			Navigator.refresh();
		});
	}
}
