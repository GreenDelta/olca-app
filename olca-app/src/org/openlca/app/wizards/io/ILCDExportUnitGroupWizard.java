package org.openlca.app.wizards.io;

import org.openlca.core.model.ModelType;

/**
 * Extension of {@link ILCDExportWizard} for unit groups
 */
public class ILCDExportUnitGroupWizard extends ILCDExportWizard {

	public ILCDExportUnitGroupWizard() {
		super(ModelType.UNIT_GROUP);
	}

}
