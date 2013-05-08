/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.preferencepages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.jobs.JobHandler;
import org.openlca.core.jobs.Jobs;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Location;
import org.openlca.core.model.Process;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.core.model.referencesearch.IReferenceSearcher;
import org.openlca.core.model.referencesearch.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Searches for references to a location
 * 
 * @see IReferenceSearcher
 * 
 * @author Sebastian Greve
 * 
 */
public class LocationReferenceSearcher implements IReferenceSearcher<Location> {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * The monitoring object
	 */
	private final JobHandler logger = Jobs.getHandler(Jobs.MAIN_JOB_HANDLER);

	@Override
	public Reference[] findReferences(final IDatabase database,
			final Location component) {
		final List<Reference> references = new ArrayList<>();
		try {
			logger.subJob(Messages.Searching);

			final Map<String, Object> properties = new HashMap<>();
			properties.put("location.id", component.getId());
			// load flow information using the location
			final List<Flow> flowsWithLocation = database.selectAll(Flow.class,
					properties);

			// for each flow information
			for (final Flow flowInformation : flowsWithLocation) {
				// get flow name
				final String name = database.selectDescriptor(Flow.class,
						flowInformation.getId()).getName();

				// create reference object
				final Reference reference = new Reference(Reference.OPTIONAL,
						Messages.Flow, name, Messages.Common_Location) {

					@Override
					public void solve() {
						// remove the location from the flow information and
						// update
						try {
							flowInformation.setLocation(null);
							database.update(flowInformation);
						} catch (final Exception e) {
							log.error("solve failed", e);
						}
					}
				};
				references.add(reference);
			} // end for

			// load process descriptors using the location
			final IModelComponent[] processesWithLocation = database
					.selectDescriptors(Process.class, properties);

			// for each process
			for (final IModelComponent object : processesWithLocation) {
				final Reference reference = new Reference(Reference.OPTIONAL,
						Messages.Process, object.getName(),
						Messages.Common_Location) {

					@Override
					public void solve() {
						try {
							// load the process
							final Process p = database.select(Process.class,
									object.getId());
							// remove location
							p.setLocation(null);
							// update process
							database.update(p);
						} catch (final Exception e) {
							log.error("solve failed", e);
						}

					}
				};
				references.add(reference);
			} // end for
		} catch (final Exception e) {
			log.error("Find references failed", e);
		}
		return references.toArray(new Reference[references.size()]);
	}
}
