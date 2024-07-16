package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.App;
import org.openlca.app.collaboration.navigation.elements.EntryElement;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.db.Libraries;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;

public class DownloadLibraryAction extends Action implements INavigationAction {

	private EntryElement elem;

	@Override
	public String getText() {
		return "Download library";
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
				() -> elem.getServer().downloadLibrary(lib));
		if (stream == null)
			return;
		App.runWithProgress("Downloading and extracting library " + lib,
				() -> Libraries.importFromStream(stream),
				Navigator::refresh);
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof EntryElement elem) || !elem.isLibrary())
			return false;
		this.elem = elem;
		return true;
	}

}
