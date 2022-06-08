package org.openlca.app.collaboration.viewers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.collaboration.views.CompareView;
import org.openlca.app.db.Repository;
import org.openlca.app.viewers.tables.AbstractTableViewer;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.git.model.Commit;
import org.openlca.git.util.Constants;

public class HistoryViewer extends AbstractTableViewer<Commit> {

	String localCommitId;
	String remoteCommitId;
	List<Commit> commits;
	HistoryImages images;

	public HistoryViewer(Composite parent) {
		super(parent);
	}

	@Override
	protected TableViewer createViewer(Composite parent) {
		TableViewer viewer = Tables.createViewer(parent, getColumnHeaders(), i -> new HistoryLabel(this, i));
		viewer.getTable().setLinesVisible(false);
		return viewer;
	}

	@Override
	protected String[] getColumnHeaders() {
		return new String[] { M.Id, M.Message, M.Committer, M.CommitDate };
	}

	@Override
	protected List<Action> getAdditionalActions() {
		var actions = new ArrayList<Action>();
		actions.add(new OpenCompareViewAction());
		return actions;
	}

	public void setRepository(Repository repo) {
		if (repo == null) {
			localCommitId = null;
			remoteCommitId = null;
			commits = Collections.emptyList();
		} else {
			localCommitId = repo.commits.resolve(Constants.LOCAL_BRANCH);
			remoteCommitId = repo.commits.resolve(Constants.REMOTE_BRANCH);
			commits = repo.commits.find()
					.refs(Constants.LOCAL_REF, Constants.REMOTE_REF).all();
			Collections.reverse(commits);
		}
		images = new HistoryImages(commits);
		super.setInput(commits);
	}

	@Override
	public void setInput(Collection<Commit> collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setInput(Commit[] input) {
		throw new UnsupportedOperationException();
	}

	private class OpenCompareViewAction extends Action {

		@Override
		public String getText() {
			return "Compare with workspace";
		}

		@Override
		public void run() {
			var commit = getSelected();
			CompareView.update(commit);
		}

	}
}