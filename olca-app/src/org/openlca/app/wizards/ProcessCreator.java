package org.openlca.app.wizards;

import java.util.UUID;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.FlowPropertyDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.core.model.ProcessType;
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
			p.refId = UUID.randomUUID().toString();
			p.name = name;
			p.description = description;
			p.lastChange = System.currentTimeMillis();
			p.processType = ProcessType.UNIT_PROCESS;
			Flow flow = getFlow();
			Exchange qRef = p.exchange(flow);
			qRef.isInput = flow.flowType == FlowType.WASTE_FLOW;
			p.quantitativeReference = qRef;
			ProcessDocumentation doc = new ProcessDocumentation();
			doc.creationDate = Calendar.getInstance().getTime();
			doc.id = p.id;
			p.documentation = doc;
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
		flow.refId = UUID.randomUUID().toString();
		if (!Strings.isNullOrEmpty(flowName))
			flow.name = flowName;
		else
			flow.name = name;
		flow.description = description;
		FlowType type = wasteProcess ? FlowType.WASTE_FLOW
				: FlowType.PRODUCT_FLOW;
		flow.flowType = type;
		FlowProperty property = new FlowPropertyDao(db).getForId(flowProperty.id);
		flow.referenceFlowProperty = property;
		FlowPropertyFactor factor = new FlowPropertyFactor();
		factor.conversionFactor = 1;
		factor.flowProperty = property;
		flow.flowPropertyFactors.add(factor);
		new FlowDao(db).insert(flow);
		return flow;
	}

}
