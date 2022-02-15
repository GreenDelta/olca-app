package org.openlca.app.navigation.actions.db;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.views.CompareView;
import org.openlca.app.collaboration.views.HistoryView;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.editors.Editors;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.DbUtils;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.config.DerbyConfig;

public class DbCopyAction extends Action implements INavigationAction {

	private DerbyConfig config;

	public DbCopyAction() {
		setText(M.Copy);
		setImageDescriptor(Icon.COPY.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement))
			return false;
		var e = (DatabaseElement) first;
		var config = e.getContent();
		if (!(config instanceof DerbyConfig))
			return false;
		this.config = (DerbyConfig) config;
		return true;
	}

	@Override
	public void run() {
		if (config == null) {
			DatabaseConfig conf = Database.getActiveConfiguration();
			if (!(conf instanceof DerbyConfig))
				return;
			config = (DerbyConfig) conf;
		}
		InputDialog dialog = new InputDialog(UI.shell(),
				M.Copy,
				M.PleaseEnterAName,
				config.name(), null);
		if (dialog.open() != Window.OK)
			return;
		String newName = dialog.getValue();
		if (!DbUtils.isValidName(newName) || Database.getConfigurations()
				.nameExists(newName.trim())) {
			MsgBox.error(M.NewDatabase_InvalidName);
			return;
		}
		App.runInUI("Copy database", () -> doCopy(newName));
	}

	private void doCopy(String newName) {
		boolean isActive = Database.isActive(config);
		try {
			if (isActive) {
				if (!Editors.closeAll())
					return;
				Database.close();
			}
			File fromFolder = DatabaseDir.getRootFolder(config.name());
			File toFolder = DatabaseDir.getRootFolder(newName);
			FileUtils.copyDirectory(fromFolder, toFolder);
			DerbyConfig newConf = new DerbyConfig();
			newConf.name(newName);
			Database.register(newConf);
			if (isActive)
				Database.activate(config);
			Navigator.refresh();
			HistoryView.refresh();
			CompareView.clear();
		} catch (Exception e) {
			ErrorReporter.on("failed to copy database", e);
		}
	}

}
