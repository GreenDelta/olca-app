package org.openlca.app.tools.mapping.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openlca.core.database.IDatabase;
import org.openlca.io.maps.FlowRef;
import org.openlca.io.maps.Status;
import org.openlca.util.Strings;

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
	public void persist(List<FlowRef> refs, IDatabase db);

	/**
	 * Synchronize the given external references with the flows from this data
	 * provider.
	 */
	public default void sync(Stream<FlowRef> externalRefs) {
		if (externalRefs == null)
			return;
		Map<String, FlowRef> packRefs = getFlowRefs().stream().collect(
				Collectors.toMap(ref -> ref.flow.refId, ref -> ref));
		externalRefs.forEach(ref -> {
			if (ref == null)
				return;
			if (ref.flow == null || ref.flow.refId == null) {
				ref.status = Status.error("missing flow reference with UUID");
				return;
			}

			// we update the status in the following sync. steps
			ref.status = null;

			// check the flow
			FlowRef packRef = packRefs.get(ref.flow.refId);
			if (packRef == null) {
				ref.status = Status.error("there is no flow with id="
						+ ref.flow.refId + " in the data package");
				return;
			}

			// check the flow property
			if (ref.property == null) {
				ref.property = packRef.property;
			} else if (packRef.property == null ||
					!Strings.nullOrEqual(
							packRef.property.refId, ref.property.refId)) {
				ref.status = Status.error("the flow in the data package has"
						+ " a different flow property");
				return;
			}

			// check the unit
			if (ref.unit == null) {
				ref.unit = packRef.unit;
			} else {
				if (packRef.unit == null) {
					ref.status = Status.error("the flow in the data package has"
							+ " no corresponding unit");
					return;
				}
				// in ILCD units have no reference IDs
				// TODO
			}

			// TODO continue with these checks
			// check the name

		});
	}
}
