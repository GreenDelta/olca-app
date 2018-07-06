package org.openlca.app.results.regionalized;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.results.ExcelExportAction;

public class RegionalizedResultActions extends EditorActionBarContributor {

	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(new ExcelExportAction());
	}

}
