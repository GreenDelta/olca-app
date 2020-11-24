package org.openlca.app.navigation.actions.cloud;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.openlca.app.M;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.ui.commits.SelectCommitDialog;
import org.openlca.app.cloud.ui.diff.CompareView;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.cloud.model.data.Commit;

public class OpenCompareViewAction extends Action implements INavigationAction {

	private final boolean selectCommit;
	private List<INavigationElement<?>> elements;

	public OpenCompareViewAction(boolean selectCommit) {
		if (selectCommit)
			setText(M.Commit);
		else
			setText(M.HEADRevision);
		this.selectCommit = selectCommit;
	}

	@Override
	public void run() {
		Commit commit = null;
		List<Commit> commits = null;
		if (selectCommit) {
			SelectCommitDialog dialog = new SelectCommitDialog();
			if (dialog.open() != IDialogConstants.OK_ID)
				return;
			commit = dialog.getSelection();
			commits = dialog.getCommits();
		}
		CompareView.update(elements, commit, CloudUtil.commitIsAhead(commit, commits));
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (!Database.isConnected())
			return false;
		this.elements = selection;
		return true;
	}

}
