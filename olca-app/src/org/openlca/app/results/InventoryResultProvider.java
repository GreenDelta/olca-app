package org.openlca.app.results;

import java.util.Collection;

import org.openlca.core.database.Cache;
import org.openlca.core.model.descriptors.FlowDescriptor;

/**
 * Provider of the single results of an inventory of a product system.
 */
public interface InventoryResultProvider {

	/** Get all flows of the inventory. */
	public Collection<FlowDescriptor> getFlows(Cache cache);

	/** Get the result for the given flow in the reference unit of that flow. */
	public double getAmount(FlowDescriptor flow);

	/** Returns true if the given flow is an input flow */
	public boolean isInput(FlowDescriptor flow);

}
