package org.openlca.app.wizards.io;

import org.openlca.core.model.ModelType;

/**
 * Extension of {@link ILCDExportWizard} for flow properties
 */
public class ILCDExportFlowPropertyWizard extends ILCDExportWizard {

	public ILCDExportFlowPropertyWizard() {
		super(ModelType.FLOW_PROPERTY);
	}

}
