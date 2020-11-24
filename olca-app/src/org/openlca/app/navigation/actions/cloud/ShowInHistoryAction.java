package org.openlca.app.navigation.actions.cloud;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.util.ErrorReporter;

public class ShowInHistoryAction extends Action implements INavigationAction {

	public ShowInHistoryAction() {
		setText(M.ShowInHistory);
	}

	@Override
	public void run() {
		IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow()
				.getActivePage();
		if (page == null)
			return;
		try {
			page.showView(HistoryView.ID);
			HistoryView.refresh();
		} catch (PartInitException e) {
			ErrorReporter.on("Error opening sync view", e);
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return Database.isConnected();
	}

}
