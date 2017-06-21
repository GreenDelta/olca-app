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

class AllocationSync {

	private final Process process;
	private boolean firstInit;

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
		new AllocationSync(process).doUpdate();
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
		doUpdate();
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
			factor.setValue(f.value);
		}
	}

	private void setNewCausalValues(List<F> factors) {
		for (F f : factors) {
			for (Exchange e : Util.getNonProviderFlows(process)) {
				AllocationFactor factor = getCausalFactor(f.product, e);
				if (factor == null)
					continue;
				factor.setValue(f.value);
			}
		}
	}

	private void doUpdate() {
		List<Exchange> pFlows = Util.getProviderFlows(process);
		if (pFlows.size() < 2) {
			process.getAllocationFactors().clear();
			return;
		}
		firstInit = process.getAllocationFactors().isEmpty();
		removeUnusedFactors(pFlows);
		addNewFactors(pFlows);
	}

	private void removeUnusedFactors(List<Exchange> products) {
		List<AllocationFactor> removals = new ArrayList<>();
		for (AllocationFactor factor : process.getAllocationFactors()) {
			long productId = factor.getProductId();
			boolean remove = true;
			for (Exchange product : products) {
				if (productId == product.getFlow().getId()) {
					remove = false;
					break;
				}
			}
			if (remove)
				removals.add(factor);
		}
		process.getAllocationFactors().removeAll(removals);
	}

	private void addNewFactors(List<Exchange> products) {
		for (Exchange product : products) {
			createIfAbsent(product, AllocationMethod.PHYSICAL);
			createIfAbsent(product, AllocationMethod.ECONOMIC);
			for (Exchange e : Util.getNonProviderFlows(process)) {
				createCausalIfAbsent(product, e);
			}
		}
	}

	/** For physical and economic allocation. */
	private void createIfAbsent(Exchange product, AllocationMethod method) {
		AllocationFactor factor = getFactor(product, method);
		if (factor != null)
			return;
		factor = new AllocationFactor();
		factor.setAllocationType(method);
		factor.setProductId(product.getFlow().getId());
		factor.setValue(getInitialValue(product));
		process.getAllocationFactors().add(factor);
	}

	private double getInitialValue(Exchange product) {
		if (firstInit && Objects.equals(product, process.getQuantitativeReference()))
			return 1;
		else
			return 0d;
	}

	/** For physical and economic allocation. */
	private AllocationFactor getFactor(Exchange product, AllocationMethod method) {
		for (AllocationFactor factor : process.getAllocationFactors()) {
			if (factor.getAllocationType() != method)
				continue;
			if (factor.getProductId() == product.getFlow().getId())
				return factor;
		}
		return null;
	}

	/** For causal allocation. */
	private void createCausalIfAbsent(Exchange product, Exchange exchange) {
		AllocationFactor factor = getCausalFactor(product, exchange);
		if (factor != null)
			return;
		factor = new AllocationFactor();
		factor.setAllocationType(AllocationMethod.CAUSAL);
		factor.setExchange(exchange);
		factor.setProductId(product.getFlow().getId());
		factor.setValue(getInitialValue(product));
		process.getAllocationFactors().add(factor);
	}

	private AllocationFactor getCausalFactor(Exchange product, Exchange exchange) {
		for (AllocationFactor factor : process.getAllocationFactors()) {
			if (factor.getAllocationType() != AllocationMethod.CAUSAL)
				continue;
			if (factor.getProductId() == product.getFlow().getId()
					&& Objects.equals(exchange, factor.getExchange()))
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
				Flow flow = product.getFlow();
				FlowPropertyFactor factor = flow.getFactor(commonProp);
				if (factor != null)
					amount = refAmount * factor.getConversionFactor();
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
		if (exchange.getUnit() == null
				|| exchange.getFlowPropertyFactor() == null)
			return 0;
		double amount = exchange.getAmountValue();
		double unitFactor = exchange.getUnit().getConversionFactor();
		double propFactor = exchange.getFlowPropertyFactor()
				.getConversionFactor();
		if (propFactor == 0)
			return 0;
		return amount * unitFactor / propFactor;
	}

	private FlowProperty getCommonProperty(List<Exchange> products,
			AllocationMethod method) {
		List<FlowProperty> candidates = null;
		for (Exchange product : products) {
			Flow flow = product.getFlow();
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
		for (FlowPropertyFactor factor : flow.getFlowPropertyFactors()) {
			FlowProperty prop = factor.getFlowProperty();
			if (match(prop.getFlowPropertyType(), method))
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
			if (product.costValue == null)
				return false;
		}
		return true;
	}

	private List<F> calculateFromCosts(List<Exchange> products) {
		List<F> factors = new ArrayList<>();
		double total = 0;
		for (Exchange product : products) {
			double val = product.costValue == null ? 0 : product.costValue;
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
