package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.dialogs.SelectCommitDialog;
import org.openlca.app.collaboration.views.CompareView;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.git.model.Commit;

class OpenCompareViewAction extends Action implements INavigationAction {

	private final boolean compareWithHead;
	private Repository repo;
	private List<INavigationElement<?>> selection;

	OpenCompareViewAction(boolean compareWithHead) {
		if (compareWithHead) {
			setText(M.HEADRevision);
		} else {
			setText(M.CommitDots);
			setImageDescriptor(Icon.COMPARE_COMMIT.descriptor());
		}
		this.compareWithHead = compareWithHead;
	}

	@Override
	public void run() {
		Commit commit = null;
		if (compareWithHead) {
			commit = repo.commits.head();
		} else {
			var dialog = new SelectCommitDialog(repo);
			if (dialog.open() != IDialogConstants.OK_ID)
				return;
			commit = dialog.getSelection();
		}
		CompareView.update(repo, commit, selection);
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		repo = Actions.getRepo(selection);
		return repo != null;
	}

}
