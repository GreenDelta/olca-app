/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.io.ui.ilcd.exporter;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.FlowProperty;
import org.openlca.io.ui.SelectObjectsExportPage;

/**
 * Extension of {@link ILCDExportWizard} for flow properties
 * 
 * @author Sebastian Greve
 * 
 */
public class ILCDExportFlowPropertyWizard extends ILCDExportWizard {

	public ILCDExportFlowPropertyWizard() {
		super(SelectObjectsExportPage.FLOW_PROPERTY);
	}

	public ILCDExportFlowPropertyWizard(final IDatabase database,
			final FlowProperty flowProperty) {
		super(SelectObjectsExportPage.FLOW_PROPERTY);
		setSingleExport(flowProperty, database);
	}

}
