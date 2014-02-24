package org.openlca.app.wizards.io;

import org.openlca.core.model.ModelType;

/**
 * Extension of {@link ILCDExportWizard} for flows
 */
public class ILCDExportFlowWizard extends ILCDExportWizard {

	public ILCDExportFlowWizard() {
		super(ModelType.FLOW);
	}

}
