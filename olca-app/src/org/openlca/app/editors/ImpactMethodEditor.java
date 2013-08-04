/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors;

import org.openlca.core.editors.IEditor;
import org.openlca.core.model.ImpactMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImpactMethodEditor extends ModelEditor<ImpactMethod> implements
		IEditor {

	public static String ID = "editors.impactmethod";
	private Logger log = LoggerFactory.getLogger(getClass());

	public ImpactMethodEditor() {
		super(ImpactMethod.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ImpactMethodInfoPage(this));
			addPage(new ImpactFactorsPage(this));
			addPage(new ImpactNormalizationWeightingPage(this));
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

}
