package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.ui.actions.*;
import org.eclipse.jface.action.*;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.editors.graphical.zoom.GraphZoomManager;

public class NotWorkingGraphActionBarContributor extends ActionBarContributor {

	@Override
	protected void buildActions() {
		addRetargetAction(new UndoRetargetAction());
		addRetargetAction(new RedoRetargetAction());

		addRetargetAction(new ZoomInRetargetAction());
		addRetargetAction(new ZoomOutRetargetAction());

		addRetargetAction(new MatchSizeRetargetAction());
		addRetargetAction(new MatchWidthRetargetAction());
		addRetargetAction(new MatchHeightRetargetAction());
	}

	@Override
	protected void declareGlobalActionKeys() {
		addGlobalActionKey(ActionFactory.SELECT_ALL.getId());
		addGlobalActionKey(ActionFactory.DELETE.getId());
	}

	@Override
	public void contributeToToolBar(IToolBarManager tbm) {
		tbm.add(getAction(ActionFactory.UNDO.getId()));
		tbm.add(getAction(ActionFactory.REDO.getId()));

		tbm.add(new Separator());
		tbm.add(getAction(GEFActionConstants.MATCH_SIZE));
		tbm.add(getAction(GEFActionConstants.MATCH_WIDTH));
		tbm.add(getAction(GEFActionConstants.MATCH_HEIGHT));

		tbm.add(new Separator());
		String[] zoomStrings = new String[] { GraphZoomManager.FIT_ALL,
			GraphZoomManager.FIT_HEIGHT, GraphZoomManager.FIT_WIDTH };
		tbm.add(new ZoomComboContributionItem(getPage(), zoomStrings));
	}

	@Override
	public void contributeToMenu(IMenuManager menubar) {
		super.contributeToMenu(menubar);
		MenuManager viewMenu = new MenuManager("&View");
		viewMenu.add(getAction(GEFActionConstants.ZOOM_IN));
		viewMenu.add(getAction(GEFActionConstants.ZOOM_OUT));
		viewMenu.add(new Separator());
		viewMenu.add(getAction(GEFActionConstants.MATCH_SIZE));
		viewMenu.add(getAction(GEFActionConstants.MATCH_WIDTH));
		viewMenu.add(getAction(GEFActionConstants.MATCH_HEIGHT));
		menubar.insertAfter(IWorkbenchActionConstants.M_EDIT, viewMenu);
	}

}
