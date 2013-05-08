package org.openlca.core.editors.controllers;

import java.util.UUID;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;

/**
 * Controller for the creation of a process from wizard data. The user can
 * decide to create a new product with the process (Data#createWithProduct =
 * true) or to set a flow as a reference flow for the process. For the first
 * case, a flow property must be provided for the second case a flow. If a flow
 * is created the flow is also inserted in the database.
 */
public class ProcessCreationController {

	private IDatabase database;
	private String name;
	private String description;
	private boolean createWithProduct;
	private FlowPropertyDescriptor flowProperty;
	private FlowDescriptor flow;
	private String categoryId;

	public ProcessCreationController(IDatabase database) {
		this.database = database;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setCreateWithProduct(boolean createWithProduct) {
		this.createWithProduct = createWithProduct;
	}

	public void setFlowProperty(FlowPropertyDescriptor flowProperty) {
		this.flowProperty = flowProperty;
	}

	public void setFlow(FlowDescriptor flow) {
		this.flow = flow;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
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
			Process process = new Process(UUID.randomUUID().toString(), name);
			process.setDescription(description);
			if (categoryId == null)
				process.setCategoryId(Process.class.getCanonicalName());
			else
				process.setCategoryId(categoryId);
			Flow flow = getFlow();
			addQuantitativeReference(process, flow);
			return process;
		} catch (Exception e) {
			throw new RuntimeException("Could not create process", e);
		}
	}

	private Flow getFlow() throws Exception {
		if (createWithProduct)
			return createFlow();
		return new FlowDao(database.getEntityFactory()).getForId(flow.getId());
	}

	private Flow createFlow() throws Exception {
		Flow flow;
		flow = new Flow(UUID.randomUUID().toString(), name);
		flow.setCategoryId(Flow.class.getCanonicalName());
		flow.setDescription(description);
		flow.setFlowType(FlowType.ProductFlow);
		FlowProperty property = database.createDao(FlowProperty.class)
				.getForId(flowProperty.getId());
		flow.setReferenceFlowProperty(property);
		flow.add(new FlowPropertyFactor(UUID.randomUUID().toString(), property,
				1));
		database.createDao(Flow.class).insert(flow);
		return flow;
	}

	private void addQuantitativeReference(Process process, Flow flow)
			throws Exception {
		Exchange qRef = new Exchange(process.getId());
		qRef.setId(UUID.randomUUID().toString());
		qRef.setFlow(flow);
		FlowProperty refProp = flow.getReferenceFlowProperty();
		qRef.setFlowPropertyFactor(flow.getFlowPropertyFactor(refProp.getId()));
		UnitGroup unitGroup = database.createDao(UnitGroup.class).getForId(
				refProp.getUnitGroupId());
		if (unitGroup != null)
			qRef.setUnit(unitGroup.getReferenceUnit());
		qRef.setInput(false);
		process.add(qRef);
		process.setQuantitativeReference(qRef);
	}
}
