package org.openlca.app.editors.actions;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;

public class ProductSystemActionContributor extends EditorActionBarContributor {

	private CalculationAction calculationAction = new CalculationAction();
	
	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(calculationAction);
	}

}
