package org.openlca.app.editors.sd.interop;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Unit;

public class SystemBinding {

	private ProductSystem system;
	private AllocationMethod allocation;
	private Unit unit;
	private FlowProperty property;
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
		return allocation;
	}

	public SystemBinding allocation(AllocationMethod allocation) {
		this.allocation = allocation;
		return this;
	}

	public Unit unit() {
		return unit;
	}

	public SystemBinding unit(Unit unit) {
		this.unit = unit;
		return this;
	}

	public FlowProperty property() {
		return property;
	}

	public SystemBinding property(FlowProperty property) {
		this.property = property;
		return this;
	}

	public Double amount() {
		return amount;
	}

	public SystemBinding amount(Double amount) {
		this.amount = amount;
		return this;
	}

	public List<VarBinding> varBindings() {
		return varBindings;
	}
}
