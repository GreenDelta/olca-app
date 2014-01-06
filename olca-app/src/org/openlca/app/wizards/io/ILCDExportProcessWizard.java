package org.openlca.app.wizards.io;

import org.openlca.core.model.ModelType;

/**
 * Extension of {@link ILCDExportWizard} for processes
 */
public class ILCDExportProcessWizard extends ILCDExportWizard {

	public ILCDExportProcessWizard() {
		super(ModelType.PROCESS);
	}

}
