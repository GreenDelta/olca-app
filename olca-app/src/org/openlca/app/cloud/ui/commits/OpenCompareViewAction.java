package org.openlca.app.cloud.ui.commits;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.ui.diff.CompareView;
import org.openlca.cloud.model.data.Commit;

class OpenCompareViewAction extends Action {

	private final HistoryViewer historyViewer;

	OpenCompareViewAction(HistoryViewer historyViewer) {
		this.historyViewer = historyViewer;
	}

	@Override
	public String getText() {
		return M.Compare;
	}

	@Override
	public void run() {
		Commit commit = historyViewer.getSelected();
		CompareView.update(null, commit, CloudUtil.commitIsAhead(commit, historyViewer.getCommits()));
	}

}
