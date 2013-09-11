package org.openlca.app.inventory;

import java.util.Collection;

import org.openlca.core.database.EntityCache;
import org.openlca.core.model.descriptors.FlowDescriptor;

/**
 * Provider of the total inventory flow results of a product system. We put
 * inventory results and analysis results behind this interface so that they can
 * use the same editor pages for the overall inventory result.
 */
public interface InventoryResultProvider {

	/** Get all flows of the inventory result. */
	public Collection<FlowDescriptor> getFlows(EntityCache cache);

	/** Get the result for the given flow in the reference unit of that flow. */
	public double getAmount(FlowDescriptor flow);

	/** Returns true if the given flow is an input flow */
	public boolean isInput(FlowDescriptor flow);

}
