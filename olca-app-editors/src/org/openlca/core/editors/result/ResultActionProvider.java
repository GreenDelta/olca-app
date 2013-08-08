/*******************************************************************************
 * Copyright (c) 2007 - 2012 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.result;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.EditorActionBarContributor;

/**
 * Action bar contributions of the result editor. See the plugin.xml.
 */
public class ResultActionProvider extends EditorActionBarContributor {

	private ExportExcelAction exportExcelAction;
	private SaveResultAction saveResultAction;

	@Override
	public void contributeToToolBar(final IToolBarManager toolBarManager) {
		saveResultAction = new SaveResultAction();
		saveResultAction.setEnabled(false);
		toolBarManager.add(saveResultAction);
		exportExcelAction = new ExportExcelAction();
		toolBarManager.add(exportExcelAction);
	}

	@Override
	public void setActiveEditor(IEditorPart targetEditor) {
		if (targetEditor instanceof ResultEditor) {
			ResultEditor e = (ResultEditor) targetEditor;
			exportExcelAction.setResults(e.getLCIResult(), e.getLCIAResult(),
					e.getDatabase());
			saveResultAction.setData(e.getLCIAResult(), e.getDatabase());
		}
	}

}
