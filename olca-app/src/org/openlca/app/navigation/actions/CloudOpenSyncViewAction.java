package org.openlca.app.navigation.actions;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.commits.SelectCommitDialog;
import org.openlca.app.cloud.ui.diff.SyncView;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.INavigationElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CloudOpenSyncViewAction extends Action implements INavigationAction {

	private final static Logger log = LoggerFactory.getLogger(CloudOpenSyncViewAction.class);
	private final boolean selectCommit;
	private List<INavigationElement<?>> elements;

	public CloudOpenSyncViewAction(boolean selectCommit) {
		if (selectCommit)
			setText(M.Commit);
		else
			setText(M.HEADRevision);
		this.selectCommit = selectCommit;
	}

	@Override
	public void run() {
		String commitId = null;
		if (selectCommit) {
			SelectCommitDialog dialog = new SelectCommitDialog();
			if (dialog.open() != IDialogConstants.OK_ID)
				return;
			commitId = dialog.getSelection().id;
		}
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (page == null)
			return;
		try {
			SyncView view = (SyncView) page.showView(SyncView.ID);
			view.update(elements, commitId);
		} catch (PartInitException e) {
			log.error("Error opening sync view", e);
		}
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!Database.isConnected())
			return false;
		this.elements = Collections.singletonList(element);
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		if (!Database.isConnected())
			return false;
		this.elements = elements;
		return true;
	}

}
