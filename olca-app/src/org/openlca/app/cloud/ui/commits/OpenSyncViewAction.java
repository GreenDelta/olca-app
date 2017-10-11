package org.openlca.app.cloud.ui.commits;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.diff.SyncView;
import org.openlca.cloud.model.data.Commit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OpenSyncViewAction extends Action {

	private final static Logger log = LoggerFactory.getLogger(OpenSyncViewAction.class);
	private final HistoryViewer historyViewer;

	OpenSyncViewAction(HistoryViewer historyViewer) {
		this.historyViewer = historyViewer;
	}

	@Override
	public String getText() {
		return M.Compare;
	}

	@Override
	public void run() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (page == null)
			return;
		try {
			Commit commit = historyViewer.getSelected();
			SyncView view = (SyncView) page.showView(SyncView.ID);
			view.update(null, commit.id);
		} catch (PartInitException e) {
			log.error("Error opening sync view", e);
		}
	}

}
