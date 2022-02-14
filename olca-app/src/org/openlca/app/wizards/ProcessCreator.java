package org.openlca.app.wizards;

import java.util.Calendar;
import java.util.UUID;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.core.model.ProcessType;

import com.google.common.base.Strings;

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
	FlowProperty flowProperty;
	Flow flow;
	String flowName;
	boolean createWithProduct;
	boolean wasteProcess;

	public ProcessCreator(IDatabase db) {
		this.db = db;
	}

	private boolean canCreate() {
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
			Flow flow = getFlow();
			Process p = Process.of(name, flow);
			p.description = description;
			p.lastChange = System.currentTimeMillis();
			p.processType = ProcessType.UNIT_PROCESS;
			var doc = new ProcessDocumentation();
			doc.creationDate = Calendar.getInstance().getTime();
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
		flow.flowType = wasteProcess
			? FlowType.WASTE_FLOW
			: FlowType.PRODUCT_FLOW;
		flow.referenceFlowProperty = flowProperty;
		var factor = new FlowPropertyFactor();
		factor.conversionFactor = 1;
		factor.flowProperty = flowProperty;
		flow.flowPropertyFactors.add(factor);
		new FlowDao(db).insert(flow);
		return flow;
	}

}
