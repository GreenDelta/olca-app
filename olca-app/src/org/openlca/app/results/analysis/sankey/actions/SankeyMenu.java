package org.openlca.app.results.analysis.sankey.actions;

import org.eclipse.jface.action.MenuManager;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;

public class SankeyMenu {

	private SankeyMenu() {
	}

	public static MenuManager create(SankeyDiagram diagram) {
		MenuManager manager = new MenuManager();
		SankeySelectionAction selectionAction = new SankeySelectionAction();
		selectionAction.setSankeyDiagram(diagram);
		manager.add(selectionAction);
		SankeyImageAction imageAction = new SankeyImageAction();
		imageAction.setSankeyDiagram(diagram);
		manager.add(imageAction);
		// ZoomManager zoom = ((ScalableRootEditPart)
		// diagram.getGraphicalViewer()
		// .getRootEditPart()).getZoomManager();
		// manager.add(new ZoomInAction(zoom));
		// manager.add(new ZoomOutAction(zoom));
		return manager;
	}

}
