/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.unitgroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.osgi.util.NLS;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.jobs.JobHandler;
import org.openlca.core.jobs.Jobs;
import org.openlca.core.model.LCIAMethod;
import org.openlca.core.model.Process;
import org.openlca.core.model.Unit;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.core.model.referencesearch.IReferenceSearcher;
import org.openlca.core.model.referencesearch.NeccessaryReference;
import org.openlca.core.model.referencesearch.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Searches for references to an unit
 * 
 * @see IReferenceSearcher
 * 
 * @author Sebastian Greve
 * 
 */
public class UnitReferenceSearcher implements IReferenceSearcher<Unit> {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * Logger for output purpose
	 */
	private final JobHandler logger = Jobs.getHandler(Jobs.MAIN_JOB_HANDLER);

	@Override
	public Reference[] findReferences(final IDatabase database,
			final Unit component) {
		final List<Reference> references = new ArrayList<>();
		try {
			// search for processes containing an exchange using the given unit
			// and create a reference object for each found
			logger.subJob(NLS.bind(Messages.Units_SearchingForWith,
					new String[] { Messages.Units_Processes,
							Messages.Units_Exchange, component.getName() }));
			final Map<String, Object> properties = new HashMap<>();
			properties.put("exchanges.unit.id", component.getId());
			final IModelComponent[] processesWithExchangesWithUnit = database
					.selectDescriptors(Process.class, properties);
			for (final IModelComponent object : processesWithExchangesWithUnit) {
				final Reference reference = new NeccessaryReference(
						Reference.REQUIRED, Messages.Units_Process,
						object.getName(), null);
				references.add(reference);
			}

			// search for LCIA methods containing an LCIA factor using the given
			// unit and create a reference object for each found
			logger.subJob(NLS.bind(Messages.Units_SearchingForWith,
					new String[] { Messages.Units_LCIAFactors,
							Messages.Units_Unit, component.getName() }));

			properties.clear();
			properties.put("lciaCategories.lciaFactors.unit.id",
					component.getId());
			final IModelComponent[] lciaMethodWithLCIACategoryWithLCIAFactorWithUnit = database
					.selectDescriptors(LCIAMethod.class, properties);
			for (final IModelComponent object : lciaMethodWithLCIACategoryWithLCIAFactorWithUnit) {
				final Reference reference = new NeccessaryReference(
						Reference.REQUIRED, Messages.Units_LCIAMethod,
						object.getName(), null);
				references.add(reference);
			}
		} catch (final Exception e) {
			log.error("Find references failed", e);
		}
		return references.toArray(new Reference[references.size()]);
	}
}
