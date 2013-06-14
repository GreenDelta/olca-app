/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.osgi.util.NLS;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.jobs.JobHandler;
import org.openlca.core.jobs.Jobs;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.core.model.referencesearch.IReferenceSearcher;
import org.openlca.core.model.referencesearch.NeccessaryReference;
import org.openlca.core.model.referencesearch.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Searches for references to an exchange
 * 
 * @see IReferenceSearcher
 * 
 * @author Sebastian Greve
 * 
 */
public class ExchangeReferenceSearcher implements IReferenceSearcher<Exchange> {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * A logger for output purpose
	 */
	private final JobHandler logger = Jobs.getHandler(Jobs.MAIN_JOB_HANDLER);

	@Override
	public Reference[] findReferences(final IDatabase database,
			final Exchange component) {
		final List<Reference> references = new ArrayList<>();
		try {
			logger.subJob(NLS.bind(Messages.Processes_SearchingForWith,
					new String[] { Messages.Processes_ProcessLinks,
							Messages.Processes_RecipientInput,
							component.getFlow().getName() }));
			final Map<String, Object> properties = new HashMap<>();
			properties.put("processLinks.recipientInput.id", component.getId());
			final IModelComponent[] productSystemsWithProcessLinkWithRecipientInput = database
					.selectDescriptors(ProductSystem.class, properties);
			for (final IModelComponent modelComponent : productSystemsWithProcessLinkWithRecipientInput) {
				final Reference reference = new NeccessaryReference(
						Reference.REQUIRED, Messages.Processes_ProductSystem,
						modelComponent.getName(), null);
				references.add(reference);
			}
			logger.subJob(NLS.bind(Messages.Processes_SearchingForWith,
					new String[] { Messages.Processes_ProcessLinks,
							Messages.Processes_ProviderOutput,
							component.getFlow().getName() }));
			properties.clear();
			properties.put("processLinks.providerOutput.id", component.getId());
			final IModelComponent[] productSystemsWithProcessLinkWithProviderOutput = database
					.selectDescriptors(ProductSystem.class, properties);
			for (final IModelComponent modelComponent : productSystemsWithProcessLinkWithProviderOutput) {
				final Reference reference = new NeccessaryReference(
						Reference.REQUIRED, Messages.Processes_ProductSystem,
						modelComponent.getName(), null);
				references.add(reference);
			}
		} catch (final Exception e) {
			log.error("Finding references failed", e);
		}
		return references.toArray(new Reference[references.size()]);
	}
}
