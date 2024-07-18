package org.openlca.app.collaboration.browse.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.browse.elements.EntryElement;
import org.openlca.app.collaboration.browse.elements.IServerNavigationElement;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.db.Libraries;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;

public class DownloadLibraryAction extends Action implements IServerNavigationAction {

	private EntryElement elem;

	@Override
	public String getText() {
		return M.DownloadLibrary;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.IMPORT.descriptor();
	}

	@Override
	public boolean isEnabled() {
		return !Workspace.getLibraryDir().hasLibrary(elem.getContent().name());
	}

	@Override
	public void run() {
		var lib = elem.getContent().name();
		var stream = WebRequests.execute(
				() -> elem.getClient().downloadLibrary(lib));
		if (stream == null)
			return;
		App.runWithProgress("Downloading and extracting library " + lib,
				() -> Libraries.importFromStream(stream),
				Navigator::refresh);
	}

	@Override
	public boolean accept(List<IServerNavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof EntryElement elem) || !elem.isLibrary())
			return false;
		this.elem = elem;
		return true;
	}

}
