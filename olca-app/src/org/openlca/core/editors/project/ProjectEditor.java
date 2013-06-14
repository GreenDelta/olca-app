/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.project;

import java.util.Calendar;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.model.Project;

/**
 * Form editor for editing project
 * 
 * @author Sebastian Greve
 * 
 */
public class ProjectEditor extends ModelEditor {

	/**
	 * The id of the project editor
	 */
	public static String ID = "org.openlca.core.editors.project.ProjectEditor";

	@Override
	protected ModelEditorPage[] initPages() {
		return new ModelEditorPage[] { new ProjectInfoPage(this),
				new ProjectComparisonPage(this) };
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {
		((Project) getModelComponent()).setLastModificationDate(Calendar
				.getInstance().getTime());
		super.doSave(monitor);
	}

}
