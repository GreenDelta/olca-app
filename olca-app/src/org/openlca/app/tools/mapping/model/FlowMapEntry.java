package org.openlca.app.tools.mapping.model;

/**
 * A FlowMapEntry describes a single mapping between two flows.
 */
public class FlowMapEntry {

	/** Describes a flow of the source system of a conversion. */
	public FlowRef sourceFlow;

	/** Describes the corresponding flow of the target system. */
	public FlowRef targetFlow;

	/**
	 * An optional conversion factor which is applied to the amounts of the source
	 * flow to convert them into the corresponding amounts of the target flow (in
	 * the respective flow properties and units); defaults to 1.0
	 */
	public double factor;

	/**
	 * Describes a synchronization result of this flow mapping with a database.
	 */
	public SyncState syncState;

	/**
	 * An additional message that gives more information about the synchronization
	 * result.
	 */
	public String syncMessage;
}
