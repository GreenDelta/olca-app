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

import org.openlca.core.model.FlowProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Form editor for editing flow properties
 */
public class FlowPropertyEditor extends ModelEditor<FlowProperty> {

	public static String ID = "editors.flowproperty";
	private Logger log = LoggerFactory.getLogger(getClass());

	public FlowPropertyEditor() {
		super(FlowProperty.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new FlowPropertyInfoPage(this));
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

}
