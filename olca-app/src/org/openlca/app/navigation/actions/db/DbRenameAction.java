package org.openlca.app.navigation.actions.db;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbRenameAction extends Action implements INavigationAction {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private DerbyConfig config;

	public DbRenameAction() {
		setText(M.Rename);
		setImageDescriptor(Icon.CHANGE.descriptor());
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
				M.Rename,
				M.PleaseEnterANewName,
				config.name(), null);
		if (dialog.open() != Window.OK)
			return;
		String newName = dialog.getValue();
		if (!DbUtils.isValidName(newName) || Database.getConfigurations()
				.nameExists(newName.trim())) {
			MsgBox.error(M.DatabaseRenameError);
			return;
		}
		doRename(newName);
	}

	private void doRename(String newName) {
		boolean isActive = Database.isActive(config);
		try {
			if (isActive) {
				if (!Editors.closeAll())
					return;
				Database.close();
			}
			File oldDbFolder = DatabaseDir.getRootFolder(config.name());
			File newDbFolder = DatabaseDir.getRootFolder(newName);
			boolean success = oldDbFolder.renameTo(newDbFolder);
			if (!success) {
				log.error("failed to rename folder");
				return;
			}
			Database.remove(config);
			config.name(newName);
			Database.register(config);
			if (isActive)
				Database.activate(config);
			Navigator.refresh();
			HistoryView.refresh();
			CompareView.clear();
		} catch (Exception e) {
			ErrorReporter.on("Failed to rename database.", e);
		}
	}
}
