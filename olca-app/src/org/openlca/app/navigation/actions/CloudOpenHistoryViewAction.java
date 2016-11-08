package org.openlca.app.navigation.actions;

import org.openlca.app.M;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.INavigationElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CloudOpenHistoryViewAction extends Action implements INavigationAction {

	private final static Logger log = LoggerFactory.getLogger(CloudOpenHistoryViewAction.class);

	public CloudOpenHistoryViewAction() {
		setText(M.ShowInHistory);
	}

	@Override
	public void run() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (page == null)
			return;
		try {
			page.showView(HistoryView.ID);
			HistoryView.refresh();
		} catch (PartInitException e) {
			log.error("Error opening sync view", e);
		}
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!Database.isConnected())
			return false;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		if (!Database.isConnected())
			return false;
		return true;
	}

}
