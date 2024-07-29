package org.openlca.app.collaboration.browse.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.collaboration.browse.elements.EntryElement;
import org.openlca.app.collaboration.browse.elements.IServerNavigationElement;
import org.openlca.app.collaboration.util.Datasets;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;

class DownloadDatasetAction extends Action implements IServerNavigationAction {

	private EntryElement elem;

	DownloadDatasetAction() {
		setText(M.ImportDataDots);
		setImageDescriptor(Icon.IMPORT.descriptor());
	}

	@Override
	public boolean isEnabled() {
		return Database.get() != null;
	}

	@Override
	public void run() {
		Datasets.download(elem.getClient(), elem.getRepositoryId(), elem.getModelType().name(), elem.getRefId());
	}

	@Override
	public boolean accept(List<IServerNavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof EntryElement elem) || !elem.isDataset())
			return false;
		this.elem = elem;
		return true;
	}

}
