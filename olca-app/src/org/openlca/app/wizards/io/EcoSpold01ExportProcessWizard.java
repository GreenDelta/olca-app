package org.openlca.app.wizards.io;

import org.openlca.core.model.ModelType;

/**
 * Extension of {@link EcoSpold01ExportWizard} for processes
 */
public class EcoSpold01ExportProcessWizard extends EcoSpold01ExportWizard {

	public EcoSpold01ExportProcessWizard() {
		super(ModelType.PROCESS);
	}

}
