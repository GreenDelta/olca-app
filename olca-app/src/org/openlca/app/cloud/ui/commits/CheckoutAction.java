package org.openlca.app.cloud.ui.commits;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Error;
import org.openlca.cloud.api.RepositoryClient;
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
		return "#Checkout...";
	}

	@Override
	public void run() {
		Database.getIndexUpdater().disable();
		DiffIndex index = Database.getDiffIndex();
		index.clear();
		Commit commit = historyViewer.getSelected();
		Runner runner = new Runner(commit.id);
		App.runWithProgress("#Checking out commit '" + commit.message + "'", runner);
		if (runner.exception != null)
			Error.showBox("#An error occured while receiving data for commit " + commit.id);
		Navigator.refresh();
		IDatabaseConfiguration db = Database.getActiveConfiguration();
		INavigationElement<?> element = Navigator.findElement(db);
		indexElement(index, element);
		index.commit();
		Database.getIndexUpdater().enable();
		Navigator.refresh();
		HistoryView.refresh();
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

	private class Runner implements Runnable {

		private final String commitId;
		private Exception exception;

		private Runner(String commitId) {
			this.commitId = commitId;
		}

		@Override
		public void run() {
			try {
				RepositoryClient client = Database.getRepositoryClient();
				client.checkout(commitId);
			} catch (WebRequestException e) {
				exception = e;
			}
		}

	}

}
