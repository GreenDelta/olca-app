package org.openlca.app.editors.sd.interop;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Unit;

public class SystemBinding {

	private ProductSystem system;
	private AllocationMethod allocation;
	private Double amount;
	private final List<VarBinding> varBindings = new ArrayList<>();

	public ProductSystem system() {
		return system;
	}

	public SystemBinding system(ProductSystem system) {
		this.system = system;
		return this;
	}

	public AllocationMethod allocation() {
		return allocation != null
				? allocation
				: AllocationMethod.USE_DEFAULT;
	}

	public SystemBinding allocation(AllocationMethod allocation) {
		this.allocation = allocation;
		return this;
	}

	public Flow flow() {
		if (system == null)
			return null;
		var ex = system.referenceExchange;
		return ex != null
				? ex.flow
				: null;
	}

	public Unit unit() {
		return system != null
				? system.targetUnit
				: null;
	}

	public FlowProperty property() {
		return system != null && system.targetFlowPropertyFactor != null
				? system.targetFlowPropertyFactor.flowProperty
				: null;
	}

	public double amount() {
		if (amount != null)
			return amount;
		return system != null
				? system.targetAmount
				: 1;
	}

	public SystemBinding amount(Double amount) {
		this.amount = amount;
		return this;
	}

	public List<VarBinding> varBindings() {
		return varBindings;
	}
}
