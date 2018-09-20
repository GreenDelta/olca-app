package org.openlca.app.navigation.actions.db;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.commits.HistoryView;
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
import org.openlca.app.util.UI;
import org.openlca.app.validation.ValidationView;
import org.openlca.core.database.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbRenameAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(getClass());

	private DerbyConfiguration config;

	public DbRenameAction() {
		setText(M.Rename);
		setImageDescriptor(Icon.CHANGE.descriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement dbElement = (DatabaseElement) element;
		IDatabaseConfiguration config = dbElement.getContent();
		if (!(config instanceof DerbyConfiguration))
			return false;
		else {
			this.config = (DerbyConfiguration) config;
			return true;
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
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
				M.Rename,
				M.PleaseEnterANewName,
				config.getName(), null);
		if (dialog.open() != Window.OK)
			return;
		String newName = dialog.getValue();
		if (!DbUtils.isValidName(newName) || Database.getConfigurations()
				.nameExists(newName.trim())) {
			org.openlca.app.util.Error
					.showBox(M.DatabaseRenameError);
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
				ValidationView.clear();
			}
			File oldDbFolder = DatabaseDir.getRootFolder(config.getName());
			File newDbFolder = DatabaseDir.getRootFolder(newName);
			boolean success = oldDbFolder.renameTo(newDbFolder);
			if (!success) {
				log.error("failed to rename folder");
				return;
			}
			Database.remove(config);
			config.setName(newName);
			Database.register(config);
			if (isActive)
				Database.activate(config);
			Navigator.refresh();
			HistoryView.refresh();
		} catch (Exception e) {
			log.error("failed to rename database", e);
		}
	}
}
