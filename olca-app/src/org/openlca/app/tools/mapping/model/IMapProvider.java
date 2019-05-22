package org.openlca.app.tools.mapping.model;

import java.io.Closeable;
import java.util.List;
import java.util.Optional;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;

/**
 * An IMapProvider describes a repository of flows and mapping definitions.
 */
public interface IMapProvider extends Closeable {

	/**
	 * Get a list of references of all flows that are contained in this provider.
	 */
	List<FlowRef> getFlowRefs();

	/**
	 * If not yet contained, persist the given flow of this provider into the given
	 * database. Returns the corresponding flow from the database or `None` when the
	 * import failed.
	 */
	public Optional<Flow> persist(FlowRef ref, IDatabase db);
}
