package org.openlca.app.processes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
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

}
