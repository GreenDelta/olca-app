package org.openlca.app.results;

import java.util.Date;
import java.util.UUID;

import org.openlca.app.db.Database;
import org.openlca.core.database.FlowDao;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.SocialAspect;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.core.results.FlowResult;

final class SaveProcessUtil {

	private IResultEditor<? extends ContributionResultProvider<?>> editor;
	private String name;
	private boolean withMetaData = false;

	private SaveProcessUtil() {
	}

	static Process create(IResultEditor<?> editor, String name,
			boolean withMetaData) {
		SaveProcessUtil util = new SaveProcessUtil();
		util.editor = editor;
		util.name = name;
		util.withMetaData = withMetaData;
		return util.create();
	}

	private Process create() {
		Process p = new Process();
		p.setName(name);
		p.setRefId(UUID.randomUUID().toString());
		p.setProcessType(ProcessType.LCI_RESULT);
		addRefFlow(p);
		addElemFlows(p);
		if (withMetaData)
			copyMetaData(p);
		return p;
	}

	private void addRefFlow(Process p) {
		CalculationSetup setup = editor.getSetup();
		Flow product = getProduct(setup);
		FlowProperty property = setup.getFlowPropertyFactor().getFlowProperty();
		Exchange qRef = p.exchange(product, property, setup.getUnit());
		qRef.amount = setup.getAmount();
		p.setQuantitativeReference(qRef);
	}

	private Flow getProduct(CalculationSetup setup) {
		if (setup == null || setup.productSystem == null)
			return null;
		Exchange ref = setup.productSystem.getReferenceExchange();
		return ref == null ? null : ref.flow;
	}

	private void addElemFlows(Process p) {
		ContributionResultProvider<?> r = editor.getResult();
		if (r == null)
			return;
		FlowDao dao = new FlowDao(Database.get());
		for (FlowDescriptor d : r.getFlowDescriptors()) {
			FlowResult result = r.getTotalFlowResult(d);
			if (result == null || result.value == 0)
				continue;
			Flow flow = dao.getForId(d.getId());
			if (flow == null)
				continue;
			Exchange e = p.exchange(flow);
			e.isInput = result.input;
			e.amount = result.value;
		}
	}

	private void copyMetaData(Process p) {
		Process refProc = editor.getSetup().productSystem
				.getReferenceProcess();
		if (refProc == null)
			return;
		for (SocialAspect sa : refProc.socialAspects)
			p.socialAspects.add(sa.clone());
		p.socialDqSystem = refProc.socialDqSystem;
		p.setCategory(refProc.getCategory());
		p.setDefaultAllocationMethod(refProc.getDefaultAllocationMethod());
		p.setDescription(refProc.getDescription());
		if (refProc.getDocumentation() != null)
			p.setDocumentation(refProc.getDocumentation().clone());
		p.setInfrastructureProcess(refProc.isInfrastructureProcess());
		p.setLastChange(new Date().getTime());
		p.setLocation(refProc.getLocation());
	}
}
