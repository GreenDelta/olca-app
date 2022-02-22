package org.openlca.app.collaboration.viewers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.collaboration.util.Format;
import org.openlca.app.collaboration.views.CompareView;
import org.openlca.app.db.Repository;
import org.openlca.app.viewers.tables.AbstractTableViewer;
import org.openlca.git.model.Commit;
import org.openlca.git.util.Constants;

public class HistoryViewer extends AbstractTableViewer<Commit> {

	private String localCommitId;
	private String remoteCommitId;

	public HistoryViewer(Composite parent) {
		super(parent);
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new HistoryLabel();
	}

	@Override
	protected String[] getColumnHeaders() {
		return new String[] { M.Id, M.Message, M.Committer, M.CommitDate };
	}

	@Override
	protected List<Action> getAdditionalActions() {
		var actions = new ArrayList<Action>();
		// actions.add(new CheckoutAction(this)); // TODO
		actions.add(new OpenCompareViewAction());
		return actions;
	}

	public void setRepository(Repository repo) {
		if (repo == null) {
			localCommitId = null;
			remoteCommitId = null;
			super.setInput(Collections.emptyList());
		} else {
			localCommitId = repo.commits.resolve(Constants.LOCAL_BRANCH);
			remoteCommitId = repo.commits.resolve(Constants.REMOTE_BRANCH);
			var commits = repo != null
					? repo.commits.find()
							.refs(Constants.LOCAL_REF, Constants.REMOTE_REF)
							.all()
					: new ArrayList<Commit>();
			Collections.reverse(commits);
			super.setInput(commits);
		}
	}

	@Override
	public void setInput(Collection<Commit> collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setInput(Commit[] input) {
		throw new UnsupportedOperationException();
	}

	class HistoryLabel extends org.eclipse.jface.viewers.LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int column) {
			var commit = (Commit) element;
			switch (column) {
			case 0:
				return commit.id;
			case 1:
				var message = commit.message;
				if (commit.id.equals(remoteCommitId)) {
					message = "REMOTE | " + message;
				}
				if (commit.id.equals(localCommitId)) {
					message = "LOCAL | " + message;
				}
				return message;
			case 2:
				return commit.user;
			case 3:
				return Format.commitDate(commit.timestamp);
			}
			return null;
		}

	}

	private class OpenCompareViewAction extends Action {

		@Override
		public String getText() {
			return M.Compare;
		}

		@Override
		public void run() {
			var commit = getSelected();
			CompareView.update(commit);
		}

	}
}