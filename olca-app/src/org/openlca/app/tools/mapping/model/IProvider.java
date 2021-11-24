package org.openlca.app.tools.mapping.model;

import java.util.List;
import java.util.stream.Stream;

import org.openlca.core.database.IDatabase;
import org.openlca.io.maps.FlowRef;

/**
 * Provides flow data from an underlying data source and implements
 * functionality for synchronizing them with a database.
 */
public interface IProvider {

	/**
	 * Get a list of all flow references from the underlying data source.
	 */
	List<FlowRef> getFlowRefs();

	/**
	 * Synchronizes the given flow references with the database.
	 */
	void persist(List<FlowRef> refs, IDatabase db);

	/**
	 * Synchronize the given external references with the flows from this data
	 * provider.
	 */
	void sync(Stream<FlowRef> externalRefs);
}
