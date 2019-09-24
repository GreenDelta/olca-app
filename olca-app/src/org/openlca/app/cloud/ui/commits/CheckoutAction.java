package org.openlca.app.cloud.ui.commits;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.index.Reindexing;
import org.openlca.app.cloud.ui.FetchNotifierMonitor;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Commit;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CheckoutAction extends Action {

	private final static Logger log = LoggerFactory.getLogger(CheckoutAction.class);
	private final HistoryViewer historyViewer;

	CheckoutAction(HistoryViewer historyViewer) {
		this.historyViewer = historyViewer;
	}

	@Override
	public String getText() {
		return M.Checkout;
	}

	@Override
	public void run() {
		if (!Question.ask(M.Checkout, M.AreYouSureYouWantToCheckout))
			return;
		Database.getIndexUpdater().disable();
		Commit commit = historyViewer.getSelected();
		try {
			doCheckout(commit);
		} catch (Exception e) {
			log.error("Error while receiving commit data", e);
			MsgBox.error(M.CommitError);
		} finally {
			Database.getIndexUpdater().enable();
			App.runWithProgress(M.RebuildingIndex, Reindexing::execute);
			Navigator.refresh();
			HistoryView.refresh();
		}
	}

	private void doCheckout(Commit commit) throws Exception {
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(UI.shell());
		dialog.run(true, false, new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor m) throws InvocationTargetException, InterruptedException {
				try {
					FetchNotifierMonitor monitor = new FetchNotifierMonitor(m, M.CheckingOutCommit);
					RepositoryClient client = Database.getRepositoryClient();
					client.checkout(commit.id, monitor);
				} catch (WebRequestException e) {
					throw new InvocationTargetException(e, e.getMessage());
				}
			}
		});
	}

}
