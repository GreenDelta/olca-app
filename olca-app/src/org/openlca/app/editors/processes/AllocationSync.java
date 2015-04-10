package org.openlca.app.editors.processes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
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

	public static void updateFactors(Process process) {
		if (process == null)
			return;
		new AllocationSync(process).doUpdate();
	}

	public static void calculateDefaults(Process process) {
		if (process == null)
			return;
		new AllocationSync(process).doCalc();
	}

	private void doCalc() {
		doUpdate();
		List<Exchange> products = Processes.getOutputProducts(process);
		if (products.size() < 2)
			return;
		List<Pair<Exchange, Double>> physFactors = calcFactors(
				AllocationMethod.PHYSICAL, products);
		List<Pair<Exchange, Double>> ecoFactors = calcFactors(
				AllocationMethod.ECONOMIC, products);
		setNewValues(physFactors, AllocationMethod.PHYSICAL);
		setNewValues(ecoFactors, AllocationMethod.ECONOMIC);
		setNewCausalValues(physFactors);
	}

	private void setNewValues(List<Pair<Exchange, Double>> newFactors,
			AllocationMethod method) {
		for (Pair<Exchange, Double> ecoFactor : newFactors) {
			Exchange product = ecoFactor.getKey();
			double value = ecoFactor.getValue();
			AllocationFactor factor = getFactor(product, method);
			if (factor == null)
				continue;
			factor.setValue(value);
		}
	}

	private void setNewCausalValues(List<Pair<Exchange, Double>> physFactors) {
		for (Pair<Exchange, Double> physFactor : physFactors) {
			Exchange product = physFactor.getKey();
			double value = physFactor.getValue();
			for (Exchange exchange : Processes.getNonOutputProducts(process)) {
				AllocationFactor causalFactor = getCausalFactor(product,
						exchange);
				if (causalFactor == null)
					continue;
				causalFactor.setValue(value);
			}
		}
	}

	private void doUpdate() {
		List<Exchange> products = Processes.getOutputProducts(process);
		if (products.size() < 2) {
			process.getAllocationFactors().clear();
			return;
		}
		firstInit = process.getAllocationFactors().isEmpty();
		removeUnusedFactors(products);
		addNewFactors(products);
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
			for (Exchange exchange : Processes.getNonOutputProducts(process)) {
				createIfAbsent(product, exchange);
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
		setValue(product, factor);
		process.getAllocationFactors().add(factor);
	}

	private void setValue(Exchange product, AllocationFactor factor) {
		double value = 0d;
		if (firstInit
				&& Objects.equals(product, process.getQuantitativeReference()))
			value = 1;
		factor.setValue(value);
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
	private void createIfAbsent(Exchange product, Exchange exchange) {
		AllocationFactor factor = getCausalFactor(product, exchange);
		if (factor != null)
			return;
		factor = new AllocationFactor();
		factor.setAllocationType(AllocationMethod.CAUSAL);
		factor.setExchange(exchange);
		factor.setProductId(product.getFlow().getId());
		setValue(product, factor);
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

	private List<Pair<Exchange, Double>> calcFactors(AllocationMethod method,
			List<Exchange> products) {
		FlowProperty commonProp = getCommonProperty(products, method);
		if (commonProp == null && method != AllocationMethod.PHYSICAL)
			commonProp = getCommonProperty(products, AllocationMethod.PHYSICAL);
		List<Pair<Exchange, Double>> amounts = new ArrayList<>();
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
			amounts.add(Pair.of(product, amount));
		}
		return makeRelative(amounts, totalAmount);
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

	private List<Pair<Exchange, Double>> makeRelative(
			List<Pair<Exchange, Double>> amounts, double totalAmount) {
		if (totalAmount == 0)
			return amounts;
		// pair is immutable, so we create a new list here
		List<Pair<Exchange, Double>> relatives = new ArrayList<>();
		for (Pair<Exchange, Double> pair : amounts) {
			double amount = pair.getValue();
			relatives.add(Pair.of(pair.getKey(), amount / totalAmount));
		}
		return relatives;
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

}
