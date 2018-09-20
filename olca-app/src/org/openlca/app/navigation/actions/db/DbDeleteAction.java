package org.openlca.app.navigation.actions.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.db.DerbyConfiguration;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.db.MySQLConfiguration;
import org.openlca.app.editors.Editors;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.app.validation.ValidationView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deletes a database. Works only for local Derby databases; remote databases
 * cannot be deleted.
 */
public class DbDeleteAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private List<IDatabaseConfiguration> configs;

	public DbDeleteAction() {
		setImageDescriptor(Icon.DELETE.descriptor());
		setDisabledImageDescriptor(Icon.DELETE_DISABLED.descriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement e = (DatabaseElement) element;
		configs = Collections.singletonList(e.getContent());
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		List<IDatabaseConfiguration> config = new ArrayList<>();
		for (INavigationElement<?> element : elements) {
			if (!(element instanceof DatabaseElement))
				return false;
			DatabaseElement e = (DatabaseElement) element;
			config.add(e.getContent());
		}
		this.configs = config;
		return true;
	}

	@Override
	public String getText() {
		return M.DeleteDatabase;
	}

	@Override
	public void run() {
		if (configs == null || configs.isEmpty()) {
			IDatabaseConfiguration config = Database.getActiveConfiguration();
			if (config == null)
				return;
			configs = Collections.singletonList(config);
		}
		if (createMessageDialog().open() != MessageDialog.OK)
			return;
		if (!checkCloseEditors())
			return;
		App.run(M.DeleteDatabase, () -> doDelete(), () -> {
			Navigator.refresh();
			HistoryView.refresh();
			ValidationView.clear();
		});
	}

	private boolean checkCloseEditors() {
		for (IDatabaseConfiguration config : this.configs) 
			if (Database.isActive(config)) 
				return Editors.closeAll();
		return true;
	}

	private void doDelete() {
		for (IDatabaseConfiguration config : this.configs) {
			try {
				tryDelete(config);
			} catch (Exception e) {
				log.error("failed to delete database", e);
			}
		}
	}

	private void tryDelete(IDatabaseConfiguration config) throws Exception {
		if (Database.isActive(config))
			Database.close();
		File dbFolder = DatabaseDir.getRootFolder(config.getName());
		if (dbFolder.isDirectory())
			FileUtils.deleteDirectory(dbFolder);
		if (config instanceof DerbyConfiguration)
			Database.remove((DerbyConfiguration) config);
		else if (config instanceof MySQLConfiguration)
			Database.remove((MySQLConfiguration) config);
	}

	private MessageDialog createMessageDialog() {
		String name = configs.size() == 1 ? configs.get(0).getName()
				: "the selected databases";
		return new MessageDialog(UI.shell(), M.Delete, null, NLS.bind(
				M.DoYouReallyWantToDelete, name),
				MessageDialog.QUESTION, new String[] { M.Yes,
						M.No, },
				MessageDialog.CANCEL);
	}

}
