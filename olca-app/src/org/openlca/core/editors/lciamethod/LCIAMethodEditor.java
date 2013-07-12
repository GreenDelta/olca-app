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

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.application.FeatureFlag;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;

/**
 * Form editor for editing LCIA methods
 * 
 * @author Sebastian Greve
 * 
 */
public class LCIAMethodEditor extends ModelEditor {

	/**
	 * The id of the LCIA method editor
	 */
	public static String ID = "org.openlca.core.editors.lciamethod.LCIAMethodEditor";

	@Override
	protected ModelEditorPage[] initPages() {
		List<ModelEditorPage> pages = new ArrayList<>();
		pages.add(new LCIAMethodInfoPage(this));
		pages.add(new LCIACategoriesPage(this));
		pages.add(new LCIAFactorsPage(this));
		pages.add(new LCIANormalizationWeightingPage(this));
		if (FeatureFlag.LOCALISED_LCIA.isEnabled())
			pages.add(new ImpactLocalisationPage(this));
		return pages.toArray(new ModelEditorPage[pages.size()]);
	}

}
