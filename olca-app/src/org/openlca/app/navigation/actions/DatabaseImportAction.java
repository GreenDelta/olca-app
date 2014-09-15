package org.openlca.app.navigation.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.Config;
import org.openlca.app.Messages;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.db.DerbyConfiguration;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.ImageType;
import org.zeroturnaround.zip.ZipUtil;

public class DatabaseImportAction extends Action implements INavigationAction {

	public DatabaseImportAction() {
		setText(Messages.ImportDatabase);
		setImageDescriptor(ImageType.DB_IO.getDescriptor());
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
			names.add(existing);
		String name = proposal;
		int count = 0;
		while (names.contains(name)) {
			count++;
			name = proposal + "_" + count;
		}
		return name;
	}

	private void realImport(File dbFolder, String dbName, File zip) {
		App.run(Messages.ImportDatabase, () -> {
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
