package org.openlca.app.results.analysis.sankey.actions;

import org.eclipse.jface.action.MenuManager;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;

public class SankeyMenu {

	private SankeyMenu() {
	}

	public static MenuManager create(SankeyDiagram diagram) {
		var manager = new MenuManager();
		manager.add(new SankeySelectionAction(diagram));
		var imageAction = new SankeyImageAction();
		imageAction.sankeyDiagram = diagram;
		manager.add(imageAction);
		manager.add(new SankeyMiniViewAction(diagram));
		manager.add(new SwitchRoutingAction(diagram));
		return manager;
	}

}
