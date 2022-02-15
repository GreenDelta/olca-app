package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.M;
import org.openlca.app.collaboration.views.HistoryView;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;

public class ShowInHistoryAction extends Action implements INavigationAction {

	public ShowInHistoryAction() {
		setText(M.ShowInHistory);
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.HISTORY_VIEW.descriptor();
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
			Actions.handleException("Error opening sync view", e);
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return Repository.isConnected();
	}

}
