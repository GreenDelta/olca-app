package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.M;
import org.openlca.app.collaboration.navigation.elements.EntryElement;
import org.openlca.app.collaboration.util.Datasets;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;

public class DownloadDatasetAction extends Action implements INavigationAction {

	private EntryElement elem;

	@Override
	public String getText() {
		return M.ImportData;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.IMPORT.descriptor();
	}

	@Override
	public boolean isEnabled() {
		return Database.get() != null;
	}

	@Override
	public void run() {
		Datasets.download(elem.getServer(), elem.getRepositoryId(), elem.getModelType().name(), elem.getRefId());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof EntryElement elem))
			return false;
		if (!elem.isDataset())
			return false;
		this.elem = elem;
		return true;
	}

}
