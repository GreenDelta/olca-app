package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;

class ShowOutlineAction extends EditorAction {

	ShowOutlineAction() {
		setId(ActionIds.SHOW_OUTLINE);
		setText(Messages.Systems_ShowOutlineAction_Text);
		setImageDescriptor(ImageType.OUTLINE_ICON.getDescriptor());
	}

	@Override
	protected boolean accept(ISelection selection) {
		return true;
	}

	@Override
	public void run() {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().showView(IPageLayout.ID_OUTLINE);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

}
