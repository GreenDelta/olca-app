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
import org.openlca.core.model.Actor;
import org.openlca.io.ui.SelectObjectsExportPage;

/**
 * The wizard for the export of actors to ILCD contact data sets.
 * 
 * @author Michael Srocka
 * 
 */
public class ILCDActorExportWizard extends ILCDExportWizard {

	public ILCDActorExportWizard() {
		super(SelectObjectsExportPage.ACTOR);
	}

	public ILCDActorExportWizard(final IDatabase database, final Actor actor) {
		super(SelectObjectsExportPage.ACTOR);
		setSingleExport(actor, database);
	}

}
