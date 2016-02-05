package org.openlca.app.results.regionalized;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;

public class RegionalizedResultActions extends EditorActionBarContributor {

	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(new ExcelExport());
	}

}
