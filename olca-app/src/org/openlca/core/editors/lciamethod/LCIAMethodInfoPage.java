/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.lciamethod;

import org.openlca.app.Messages;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorInfoPage;
import org.openlca.core.model.ImpactMethod;

/**
 * Form page for editing actors
 * 
 * @author Sebastian Greve
 * 
 */
public class LCIAMethodInfoPage extends ModelEditorInfoPage {

	/**
	 * the LCIA method object edited by this editor
	 */
	private ImpactMethod lciaMethod = null;

	/**
	 * Creates a new instance.
	 * 
	 * @param editor
	 *            the editor of this page
	 */
	public LCIAMethodInfoPage(final ModelEditor editor) {
		super(editor, "LCIAMethodInfoPage", Messages.Common_GeneralInformation,
				Messages.Common_GeneralInformation);
		this.lciaMethod = (ImpactMethod) editor.getModelComponent();
	}

	@Override
	protected String getFormTitle() {
		final String title = Messages.Common_LCIAMethodTitle
				+ ": "
				+ (lciaMethod != null ? lciaMethod.getName() != null ? lciaMethod
						.getName() : ""
						: "");
		return title;
	}

	@Override
	public void dispose() {
		super.dispose();
		lciaMethod = null;
	}

}
