package org.openlca.core.editors.productsystem;

import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.NormalizationWeightingSet;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;

class CalculationSettings {

	private NormalizationWeightingSet nwSet;
	private ImpactMethodDescriptor method;
	private AllocationMethod allocationMethod;
	private CalculationType type;
	private int iterationCount;

	public NormalizationWeightingSet getNwSet() {
		return nwSet;
	}

	public void setNwSet(NormalizationWeightingSet nwSet) {
		this.nwSet = nwSet;
	}

	public ImpactMethodDescriptor getMethod() {
		return method;
	}

	public void setMethod(ImpactMethodDescriptor method) {
		this.method = method;
	}

	public AllocationMethod getAllocationMethod() {
		return allocationMethod;
	}

	public void setAllocationMethod(AllocationMethod allocationMethod) {
		this.allocationMethod = allocationMethod;
	}

	public CalculationType getType() {
		return type;
	}

	public void setType(CalculationType type) {
		this.type = type;
	}

	public void setIterationCount(int iterationCount) {
		this.iterationCount = iterationCount;
	}

	public int getIterationCount() {
		return iterationCount;
	}

}
