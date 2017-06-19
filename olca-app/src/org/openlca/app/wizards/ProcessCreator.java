package org.openlca.app.wizards;

import java.util.UUID;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;

import com.google.common.base.Strings;
import com.ibm.icu.util.Calendar;

/**
 * Controller for the creation of a process from wizard data. The user can
 * decide to create a new product with the process (Data#createWithProduct =
 * true) or to set a flow as a reference flow for the process. For the first
 * case, a flow property must be provided for the second case a flow. If a flow
 * is created the flow is also inserted in the database.
 */
class ProcessCreator {

	private final IDatabase db;

	String name;
	String description;
	BaseDescriptor flowProperty;
	Flow flow;
	String flowName;
	boolean createWithProduct;
	boolean wasteProcess;

	public ProcessCreator(IDatabase db) {
		this.db = db;
	}

	public boolean canCreate() {
		if (name == null || name.trim().isEmpty())
			return false;
		if (createWithProduct)
			return flowProperty != null;
		return flow != null;
	}

	public Process create() {
		if (!canCreate())
			throw new RuntimeException("Invalid arguments for process creation");
		try {
			Process p = new Process();
			p.setRefId(UUID.randomUUID().toString());
			p.setName(name);
			p.setDescription(description);
			p.setLastChange(System.currentTimeMillis());
			p.setProcessType(ProcessType.UNIT_PROCESS);
			Flow flow = getFlow();
			addQuantitativeReference(p, flow);
			ProcessDocumentation doc = new ProcessDocumentation();
			doc.setCreationDate(Calendar.getInstance().getTime());
			doc.setId(p.getId());
			p.setDocumentation(doc);
			return p;
		} catch (Exception e) {
			throw new RuntimeException("Could not create process", e);
		}
	}

	private Flow getFlow() {
		if (createWithProduct)
			return createFlow();
		return flow;
	}

	private Flow createFlow() {
		Flow flow = new Flow();
		flow.setRefId(UUID.randomUUID().toString());
		if (!Strings.isNullOrEmpty(flowName))
			flow.setName(flowName);
		else
			flow.setName(name);
		flow.setDescription(description);
		FlowType type = wasteProcess ? FlowType.WASTE_FLOW
				: FlowType.PRODUCT_FLOW;
		flow.setFlowType(type);
		FlowProperty property = db.createDao(FlowProperty.class)
				.getForId(flowProperty.getId());
		flow.setReferenceFlowProperty(property);
		FlowPropertyFactor factor = new FlowPropertyFactor();
		factor.setConversionFactor(1);
		factor.setFlowProperty(property);
		flow.getFlowPropertyFactors().add(factor);
		db.createDao(Flow.class).insert(flow);
		return flow;
	}

	private void addQuantitativeReference(Process process, Flow flow) {
		Exchange qRef = new Exchange();
		qRef.setAmountValue(1.0);
		qRef.setFlow(flow);
		FlowProperty refProp = flow.getReferenceFlowProperty();
		qRef.setFlowPropertyFactor(flow.getReferenceFactor());
		UnitGroup unitGroup = refProp.getUnitGroup();
		if (unitGroup != null)
			qRef.setUnit(unitGroup.getReferenceUnit());
		qRef.setInput(flow.getFlowType() == FlowType.WASTE_FLOW);
		process.getExchanges().add(qRef);
		process.setQuantitativeReference(qRef);
	}
}
