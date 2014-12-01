package org.openlca.app.wizards.io;

import org.openlca.core.model.ModelType;

/**
 * Extension of {@link EcoSpold01ExportWizard} for impact methods
 */
public class EcoSpold01ExportMethodWizard extends EcoSpold01ExportWizard {

	public EcoSpold01ExportMethodWizard() {
		super(ModelType.IMPACT_METHOD);
	}

}
