/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.processes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.openlca.app.Messages;
import org.openlca.app.util.Error;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;

/**
 * Property source for the {@link ExchangePropertiesPage}
 */
public class ExchangePropertySource implements IPropertySource {

	private Exchange exchange;
	private Process process;
	private UncertaintyPropertyProvider uncertaintyProvider;

	public ExchangePropertySource(Exchange exchange, Process process) {
		this.exchange = exchange;
		this.process = process;
		this.uncertaintyProvider = new UncertaintyPropertyProvider(exchange);
	}

	@Override
	public Object getEditableValue() {
		return exchange;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		List<IPropertyDescriptor> descriptors = new ArrayList<>();
		descriptors.addAll(uncertaintyProvider.getDescriptors());
		if (shouldCreateAllocationDescriptors()) {
			for (Exchange exchange : process.getOutputs(FlowType.PRODUCT_FLOW))
				createAllocationDescriptor(descriptors, exchange);
			for (Exchange exchange : process.getOutputs(FlowType.WASTE_FLOW))
				createAllocationDescriptor(descriptors, exchange);
		}
		IPropertyDescriptor[] result = new IPropertyDescriptor[descriptors
				.size()];
		descriptors.toArray(result);
		return result;
	}

	private boolean shouldCreateAllocationDescriptors() {
		return (process.getAllocationMethod() == AllocationMethod.Causal)
				&& (this.exchange.isInput() || this.exchange.getFlow()
						.getFlowType() == FlowType.ELEMENTARY_FLOW);
	}

	private void createAllocationDescriptor(
			List<IPropertyDescriptor> descriptors, Exchange exchange) {
		TextPropertyDescriptor descriptor = new TextPropertyDescriptor("a"
				+ exchange.getId(), exchange.getFlow().getName());
		descriptor.setCategory(Messages.AllocationFactors);
		descriptors.add(descriptor);
	}

	@Override
	public Object getPropertyValue(Object id) {
		Object object = null;
		if (uncertaintyProvider.hasPropertyValue(id)) {
			object = uncertaintyProvider.getPropertyValue(id);
		} else if (id.toString().startsWith("a")) {
			AllocationFactor factor = exchange.getAllocationFactor(id
					.toString().substring(1));
			if (factor == null) {
				factor = new AllocationFactor(UUID.randomUUID().toString(), id
						.toString().substring(1), 0);
				exchange.add(factor, false);
			}
			object = Double.toString(factor.getValue());
		}
		return object;
	}

	@Override
	public boolean isPropertySet(Object id) {
		return false;
	}

	@Override
	public void resetPropertyValue(Object id) {

	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		if (uncertaintyProvider.hasPropertyValue(id))
			uncertaintyProvider.setPropertyValue(id, value);
		else if (id.toString().startsWith("a")) {
			String valStr = value.toString();
			AllocationFactor factor = exchange.getAllocationFactor(id
					.toString().substring(1));
			try {
				double d = Double.parseDouble(valStr);
				factor.setValue(d);
			} catch (Exception e) {
				Error.showPopup("Only numbers allowed", valStr
						+ " is not a valid number");
			}
		}
	}
}
