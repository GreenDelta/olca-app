package org.openlca.app.editors.processes.allocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowPropertyType;
import org.openlca.core.model.Process;
import org.openlca.util.AllocationCleanup;

class AllocationSync {

	private final Process process;

	private AllocationSync(Process process) {
		this.process = process;
	}

	/**
	 * Adds or removes allocation factors if required. New allocation factors
	 * are initialized with a default value. Values of existing allocation
	 * factors are not changed.
	 */
	public static void updateFactors(Process process) {
		if (process == null)
			return;
		AllocationCleanup.on(process);
	}

	/**
	 * Calculates default allocation factors from the flow properties or costs
	 * of the exchanges. It also calls updateFactors internally.
	 */
	public static void calculateDefaults(Process process) {
		if (process == null)
			return;
		new AllocationSync(process).doCalc();
	}

	private void doCalc() {
		AllocationCleanup.on(process);
		List<Exchange> pFlows = Util.getProviderFlows(process);
		if (pFlows.size() < 2)
			return;
		List<F> physFactors = calcFactors(AllocationMethod.PHYSICAL, pFlows);
		List<F> ecoFactors;
		if (canCalculateFromCosts(pFlows))
			ecoFactors = calculateFromCosts(pFlows);
		else
			ecoFactors = calcFactors(AllocationMethod.ECONOMIC, pFlows);
		setNewValues(physFactors, AllocationMethod.PHYSICAL);
		setNewValues(ecoFactors, AllocationMethod.ECONOMIC);
		setNewCausalValues(physFactors);
	}

	private void setNewValues(List<F> factors, AllocationMethod method) {
		for (F f : factors) {
			AllocationFactor factor = getFactor(f.product, method);
			if (factor == null)
				continue;
			factor.value = f.value;
		}
	}

	private void setNewCausalValues(List<F> factors) {
		for (F f : factors) {
			for (Exchange e : Util.getNonProviderFlows(process)) {
				AllocationFactor factor = getCausalFactor(f.product, e);
				if (factor == null)
					continue;
				factor.value = f.value;
			}
		}
	}

	/** For physical and economic allocation. */
	private AllocationFactor getFactor(Exchange product, AllocationMethod method) {
		for (AllocationFactor factor : process.allocationFactors) {
			if (factor.method != method)
				continue;
			if (factor.productId == product.flow.id)
				return factor;
		}
		return null;
	}

	private AllocationFactor getCausalFactor(Exchange product, Exchange exchange) {
		for (AllocationFactor factor : process.allocationFactors) {
			if (factor.method != AllocationMethod.CAUSAL)
				continue;
			if (factor.productId == product.flow.id
					&& Objects.equals(exchange, factor.exchange))
				return factor;
		}
		return null;
	}

	private List<F> calcFactors(AllocationMethod method, List<Exchange> products) {
		FlowProperty commonProp = getCommonProperty(products, method);
		if (commonProp == null && method != AllocationMethod.PHYSICAL)
			commonProp = getCommonProperty(products, AllocationMethod.PHYSICAL);
		List<F> factors = new ArrayList<>();
		double totalAmount = 0;
		for (Exchange product : products) {
			double refAmount = getRefAmount(product);
			double amount = 0;
			if (commonProp != null) {
				Flow flow = product.flow;
				FlowPropertyFactor factor = flow.getFactor(commonProp);
				if (factor != null)
					amount = refAmount * factor.conversionFactor;
			}
			totalAmount += amount;
			factors.add(new F(product, amount));
		}
		if (totalAmount == 0)
			return factors;
		for (F f : factors)
			f.value = f.value / totalAmount;
		return factors;
	}

	private double getRefAmount(Exchange exchange) {
		if (exchange.unit == null
				|| exchange.flowPropertyFactor == null)
			return 0;
		double amount = exchange.amount;
		double unitFactor = exchange.unit.conversionFactor;
		double propFactor = exchange.flowPropertyFactor.conversionFactor;
		if (propFactor == 0)
			return 0;
		return amount * unitFactor / propFactor;
	}

	private FlowProperty getCommonProperty(List<Exchange> products,
			AllocationMethod method) {
		List<FlowProperty> candidates = null;
		for (Exchange product : products) {
			Flow flow = product.flow;
			List<FlowProperty> props = getProperties(flow, method);
			if (candidates == null)
				candidates = props;
			else
				candidates.retainAll(props);
		}
		if (candidates == null || candidates.isEmpty())
			return null;
		return candidates.get(0);
	}

	private List<FlowProperty> getProperties(Flow flow, AllocationMethod method) {
		List<FlowProperty> properties = new ArrayList<>();
		for (FlowPropertyFactor factor : flow.flowPropertyFactors) {
			FlowProperty prop = factor.flowProperty;
			if (match(prop.flowPropertyType, method))
				properties.add(prop);
		}
		return properties;
	}

	private boolean match(FlowPropertyType propertyType, AllocationMethod method) {
		if (propertyType == null || method == null)
			return false;
		else if (propertyType == FlowPropertyType.ECONOMIC
				&& method == AllocationMethod.ECONOMIC)
			return true;
		else if (propertyType == FlowPropertyType.PHYSICAL
				&& method == AllocationMethod.PHYSICAL)
			return true;
		else
			return false;
	}

	private boolean canCalculateFromCosts(List<Exchange> products) {
		for (Exchange product : products) {
			if (product.costs == null)
				return false;
		}
		return true;
	}

	private List<F> calculateFromCosts(List<Exchange> products) {
		List<F> factors = new ArrayList<>();
		double total = 0;
		for (Exchange product : products) {
			double val = product.costs == null ? 0 : product.costs;
			if (product.currency != null) {
				val *= product.currency.conversionFactor;
			}
			factors.add(new F(product, val));
			total += val;
		}
		if (total == 0)
			return factors;
		for (F f : factors)
			f.value = f.value / total;
		return factors;
	}

	/** Simple internal class that represents an allocation factor. */
	private class F {
		final Exchange product;
		double value;

		F(Exchange product, double value) {
			this.product = product;
			this.value = value;
		}
	}

}
