/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.source;

import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;

/**
 * Form editor for editing sources
 * 
 * @author Sebastian Greve
 * 
 */
public class SourceEditor extends ModelEditor {

	/**
	 * The id of the source editor
	 */
	public static String ID = "org.openlca.core.editors.source.SourceEditor";

	@Override
	protected ModelEditorPage[] initPages() {
		return new ModelEditorPage[] { new SourceInfoPage(this) };
	}

}
