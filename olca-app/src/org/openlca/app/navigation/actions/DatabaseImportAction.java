package org.openlca.app.navigation.actions;

import java.io.File;
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
import org.openlca.app.resources.ImageType;
import org.openlca.util.Strings;
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
		String dbName = file.getName().replace(".zolca", "")
				.replaceAll(" ", "_").toLowerCase();
		File dbFolder = new File(App.getWorkspace(),
				Config.DATABASE_FOLDER_NAME);
		if (!dbFolder.exists())
			dbFolder.mkdirs();
		int number = numberOfSameNames(dbName, dbFolder);
		if (number > 0)
			dbName = dbName + "_" + number;
		realImport(dbFolder, dbName, file);
	}

	private int numberOfSameNames(String dbName, File dbFolder) {
		int count = 0;
		for (String other : dbFolder.list()) {
			if (other == null)
				continue;
			if (Strings.nullOrEqual(dbName, other))
				count++;
			else if (other.startsWith(dbName + "_"))
				count++;
		}
		return count;
	}

	private void realImport(final File dbFolder, final String dbName,
			final File zip) {
		App.run(Messages.ImportDatabase, new Runnable() {
			public void run() {
				File folder = new File(dbFolder, dbName);
				folder.mkdirs();
				ZipUtil.unpack(zip, folder);
			}
		}, new Runnable() {
			public void run() {
				DerbyConfiguration conf = new DerbyConfiguration();
				conf.setFolder(dbFolder);
				conf.setName(dbName);
				Database.register(conf);
				Navigator.refresh();
			}
		});
	}
}
