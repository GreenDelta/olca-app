package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.SelectCommitDialog;
import org.openlca.app.collaboration.views.CompareView;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.git.model.Commit;

public class OpenCompareViewAction extends Action implements INavigationAction {

	private final boolean compareWithHead;
	private List<INavigationElement<?>> selection;

	public OpenCompareViewAction(boolean compareWithHead) {
		if (compareWithHead) {
			setText(M.HEADRevision);
		} else {
			setText(M.Commit + "...");
			setImageDescriptor(Icon.COMPARE_COMMIT.descriptor());
		}
		this.compareWithHead = compareWithHead;
	}

	@Override
	public void run() {
		Commit commit = null;
		if (compareWithHead) {
			commit = Repository.get().commits.head();
		} else {
			SelectCommitDialog dialog = new SelectCommitDialog();
			if (dialog.open() != IDialogConstants.OK_ID)
				return;
			commit = dialog.getSelection();
		}
		CompareView.update(commit, selection);
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (!Repository.isConnected())
			return false;
		this.selection = selection;
		return true;
	}

}
