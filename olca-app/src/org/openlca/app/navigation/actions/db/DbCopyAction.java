package org.openlca.app.navigation.actions.db;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.cloud.ui.diff.CompareView;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.db.DerbyConfiguration;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.editors.Editors;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.validation.ValidationView;
import org.openlca.core.database.DbUtils;

public class DbCopyAction extends Action implements INavigationAction {

	private DerbyConfiguration config;

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
		if (!(config instanceof DerbyConfiguration))
			return false;
		this.config = (DerbyConfiguration) config;
		return true;
	}

	@Override
	public void run() {
		if (config == null) {
			IDatabaseConfiguration conf = Database.getActiveConfiguration();
			if (!(conf instanceof DerbyConfiguration))
				return;
			config = (DerbyConfiguration) conf;
		}
		InputDialog dialog = new InputDialog(UI.shell(),
				M.Copy,
				M.PleaseEnterAName,
				config.getName(), null);
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
				ValidationView.clear();
			}
			File fromFolder = DatabaseDir.getRootFolder(config.getName());
			File toFolder = DatabaseDir.getRootFolder(newName);
			FileUtils.copyDirectory(fromFolder, toFolder);
			DerbyConfiguration newConf = new DerbyConfiguration();
			newConf.setName(newName);
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
