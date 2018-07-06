package org.openlca.app.results.analysis;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.results.ExcelExportAction;

public class AnalyzeResultActions extends EditorActionBarContributor {

	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(new ExcelExportAction());
	}

}
