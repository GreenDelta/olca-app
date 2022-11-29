package org.openlca.app.tools.graphics;

import org.eclipse.gef.ui.actions.ActionBarContributor;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.MatchWidthRetargetAction;
import org.eclipse.gef.ui.actions.ZoomInRetargetAction;
import org.eclipse.gef.ui.actions.ZoomOutRetargetAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.tools.graphics.actions.retarget.FocusRetargetAction;
import org.openlca.app.tools.graphics.actions.retarget.MinimapRetargetAction;
import org.openlca.app.tools.graphics.actions.ActionIds;
import org.openlca.app.tools.graphics.actions.retarget.SaveImageRetargetAction;
import org.openlca.app.tools.graphics.frame.GraphicalEditorWithFrame;

public class BasicActionBarContributor extends ActionBarContributor {

	private final GraphicalEditorWithFrame editor;
	private MenuManager viewMenu;
	private MenuManager editMenu;

	public BasicActionBarContributor(GraphicalEditorWithFrame editor) {
		this.editor = editor;
	}

	@Override
	protected void buildActions() {
		addRetargetAction(new ZoomInRetargetAction());
		addRetargetAction(new ZoomOutRetargetAction());
		addRetargetAction(new MatchWidthRetargetAction());
		addRetargetAction(new FocusRetargetAction());
		addRetargetAction(new MinimapRetargetAction(editor.getMinimap().isVisible()));
		addRetargetAction(new SaveImageRetargetAction());
	}

	@Override
	protected void declareGlobalActionKeys() {
		addGlobalActionKey(ActionFactory.SELECT_ALL.getId());
		addGlobalActionKey(ActionFactory.DELETE.getId());
	}

	@Override
	public void contributeToToolBar(IToolBarManager tbm) {
		tbm.add(getAction(GEFActionConstants.ZOOM_IN));
		tbm.add(getAction(GEFActionConstants.ZOOM_OUT));
		tbm.add(getAction(GEFActionConstants.MATCH_WIDTH));
		tbm.add(getAction(ActionIds.FOCUS));
		tbm.add(getAction(ActionIds.SAVE_IMAGE));
		tbm.add(getAction(ActionIds.MINIMAP));
	}

	@Override
	public void contributeToMenu(IMenuManager menuManager) {
		super.contributeToMenu(menuManager);
		contributeToViewMenu(menuManager);
		contributeToEditMenu(menuManager);
	}

	public void contributeToViewMenu(IMenuManager menuManager) {
		viewMenu = new MenuManager("View");
		viewMenu.add(getAction(GEFActionConstants.ZOOM_IN));
		viewMenu.add(getAction(GEFActionConstants.ZOOM_OUT));
		viewMenu.add(new Separator());
		viewMenu.add(getAction(GEFActionConstants.MATCH_WIDTH));
		viewMenu.add(getAction(ActionIds.FOCUS));
		viewMenu.add(getAction(ActionIds.SAVE_IMAGE));
		viewMenu.add(getAction(ActionIds.MINIMAP));
		menuManager.insertAfter("File", viewMenu);
	}

	public void contributeToEditMenu(IMenuManager menuManager) {
		editMenu = new MenuManager("Edit");
		menuManager.insertAfter("File", editMenu);
	}

	public MenuManager getViewMenu() {
		return viewMenu;
	}

	public MenuManager getEditMenu() {
		return editMenu;
	}

	public GraphicalEditorWithFrame getEditor() {
		return editor;
	}

}
