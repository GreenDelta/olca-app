/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.unitgroup;

import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;

/**
 * Form editor for editing unit groups
 * 
 * @author Sebastian Greve
 * 
 */
public class UnitGroupEditor extends ModelEditor {

	/**
	 * The id of the unit group editor
	 */
	public static String ID = "org.openlca.core.editors.unitgroup.UnitGroupEditor";

	@Override
	protected ModelEditorPage[] initPages() {
		return new ModelEditorPage[] { new UnitGroupInfoPage(this) };
	}
}
