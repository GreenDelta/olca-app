package org.openlca.app.results.quick;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.results.ExcelExportAction;

public class QuickResultActions extends EditorActionBarContributor {

	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(new ExcelExportAction());
	}

}
