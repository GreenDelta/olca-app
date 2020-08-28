package org.openlca.app.results;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;

public class ResultEditorActions extends EditorActionBarContributor {
	@Override
	public void contributeToToolBar(IToolBarManager toolbar) {
		toolbar.add(new ExcelExportAction());
	}
}
