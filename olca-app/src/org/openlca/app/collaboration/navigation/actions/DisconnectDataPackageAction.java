package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DataPackageElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Question;
import org.openlca.core.database.DataPackage;
import org.openlca.core.library.Unmounter;

public class DisconnectDataPackageAction extends Action implements INavigationAction {

	public DataPackage dataPackage;

	@Override
	public String getText() {
		return M.Disconnect;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.DISCONNECT_REPOSITORY.descriptor();
	}

	@Override
	public void run() {
		Runnable action = () -> Unmounter.keepNone(Database.get(), dataPackage.name());
		var answer = Question.ask(M.Disconnect, M.UnmountQuestion,
				new String[] { M.Cancel, M.KeepAll, M.KeepUsed, M.DeleteAll });
		if (answer == 0)
			return;
		if (answer == 1) {
			action = () -> Unmounter.keepAll(Database.get(), dataPackage.name());
		} else if (answer == 2) {
			action = () -> Unmounter.keepUsed(Database.get(), dataPackage.name());
		}
		App.runWithProgress(M.DisconnectingRepository, action, () -> {
			Repository.delete(Database.get().getName(), dataPackage);
			Navigator.refresh();
		});
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DataPackageElement elem))
			return false;
		if (!elem.getDatabase().isPresent() || elem.getContent().isLibrary())
			return false;
		dataPackage = elem.getContent();
		return true;
	}

}
