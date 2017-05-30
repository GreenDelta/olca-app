package org.openlca.app.cloud.ui.commits;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.M;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.ui.FetchNotifierMonitor;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Error;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.api.RepositoryConfig;
import org.openlca.cloud.model.data.Commit;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.util.WebRequests.WebRequestException;

class CheckoutAction extends Action {

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
		DiffIndex index = Database.getDiffIndex();
		index.clear();
		Commit commit = historyViewer.getSelected();
		try {
			doCheckout(commit);
		} catch (Exception e) {
			Error.showBox(M.AnErrorOccuredWhileReceivingCommitData);
		} finally {
			RepositoryConfig config = Database.getRepositoryClient().getConfig();
			Database.disconnect();
			Navigator.refresh();
			IDatabaseConfiguration db = Database.getActiveConfiguration();
			INavigationElement<?> element = Navigator.findElement(db);
			config = RepositoryConfig.connect(Database.get(), config.baseUrl, config.repositoryId, config.credentials);
			Database.connect(new RepositoryClient(config));
			indexElement(index, element);
			index.commit();
			Database.getIndexUpdater().enable();
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
					throw new InvocationTargetException(e);
				}
			}
		});
	}

	private void indexElement(DiffIndex index, INavigationElement<?> element) {
		long id = 0;
		if (element instanceof CategoryElement)
			id = ((CategoryElement) element).getContent().getId();
		if (element instanceof ModelElement)
			id = ((ModelElement) element).getContent().getId();
		if (id != 0l) {
			Dataset dataset = CloudUtil.toDataset(element);
			index.add(dataset, id);
		}
		for (INavigationElement<?> child : element.getChildren())
			indexElement(index, child);
	}

}
