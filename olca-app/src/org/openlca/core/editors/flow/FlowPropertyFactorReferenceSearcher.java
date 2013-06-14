/*******************************************************************************
 * Copyright (c) 2007 - 2012 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.osgi.util.NLS;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.Query;
import org.openlca.core.jobs.JobHandler;
import org.openlca.core.jobs.Jobs;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.referencesearch.IReferenceSearcher;
import org.openlca.core.model.referencesearch.NeccessaryReference;
import org.openlca.core.model.referencesearch.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Searches for references to a flow property factor
 * 
 * @see IReferenceSearcher
 */
public class FlowPropertyFactorReferenceSearcher implements
		IReferenceSearcher<FlowPropertyFactor> {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private JobHandler jobHandler = Jobs.getHandler(Jobs.MAIN_JOB_HANDLER);

	@Override
	public Reference[] findReferences(IDatabase database,
			FlowPropertyFactor factor) {
		List<Reference> references = new ArrayList<>();
		try {
			showSearchMessageFor(Messages.Flows_Exchanges, factor);
			addProcessReferences(database, factor, references);
			showSearchMessageFor(Messages.Common_LCIAFactors, factor);
			addMethodReferences(database, factor, references);
		} catch (Exception e) {
			log.error("Finding references failed", e);
		}
		return references.toArray(new Reference[references.size()]);
	}

	private void showSearchMessageFor(String what, FlowPropertyFactor factor) {
		jobHandler.subJob(NLS.bind(Messages.Flows_SearchingForWith,
				new String[] { what, Messages.Common_FlowProperty,
						factor.getFlowProperty().getName() }));
	}

	private void addMethodReferences(IDatabase database,
			FlowPropertyFactor factor, List<Reference> references)
			throws Exception {
		String jpql = "select m.name from LCIAMethod m join m.lciaCategories"
				+ " c join c.lciaFactors f where f.flowPropertyFactor = :factor";
		Map<String, Object> params = new HashMap<>();
		params.put("factor", factor);
		List<String> methodNames = Query.on(database).getAll(String.class,
				jpql, params);
		for (String methodName : methodNames) {
			Reference reference = new NeccessaryReference(Reference.REQUIRED,
					Messages.Common_LCIAMethodTitle, methodName,
					"lciaFactor.flowPropertyFactor");
			references.add(reference);
		}
	}

	private void addProcessReferences(IDatabase database,
			FlowPropertyFactor factor, List<Reference> references)
			throws Exception {
		String jpql = "select p.name from Process p join p.exchanges e where "
				+ "e.flowPropertyFactor = :factor";
		Map<String, Object> params = new HashMap<>();
		params.put("factor", factor);
		List<String> processNames = Query.on(database).getAll(String.class,
				jpql, params);
		for (String processName : processNames) {
			Reference reference = new NeccessaryReference(Reference.REQUIRED,
					Messages.Common_Process, processName,
					"exchange.flowPropertyFactor");
			references.add(reference);
		}
	}
}
