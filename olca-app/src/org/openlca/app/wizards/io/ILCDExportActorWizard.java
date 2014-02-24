package org.openlca.app.wizards.io;

import org.openlca.core.model.ModelType;

/**
 * The wizard for the export of actors to ILCD contact data sets.
 */
public class ILCDExportActorWizard extends ILCDExportWizard {

	public ILCDExportActorWizard() {
		super(ModelType.ACTOR);
	}

}
