package org.openlca.app.tools.mapping.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openlca.app.util.Fn;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.FlowPropertyDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.LocationDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.io.maps.FlowRef;
import org.openlca.io.maps.MappingStatus;
import org.openlca.util.Categories;

public class DBProvider implements IProvider {

	private final IDatabase db;

	public DBProvider(IDatabase db) {
		this.db = db;
	}

	public IDatabase db() {
		return db;
	}

	@Override
	public List<FlowRef> getFlowRefs() {

		// collect categories, properties, locations
		var categories = Categories.pathsOf(db);
		Map<Long, FlowProperty> props = new FlowPropertyDao(db)
			.getAll().stream()
			.collect(Collectors.toMap(fp -> fp.id, fp -> fp));
		Map<Long, String> locations = new LocationDao(db).getCodes();

		List<FlowRef> refs = new ArrayList<>();
		new FlowDao(db).getDescriptors().forEach(flow -> {
			FlowRef ref = new FlowRef();
			ref.flow = flow;
			ref.flowCategory = categories.pathOf(flow.category);
			ref.flowLocation = locations.get(flow.location);
			Fn.with(props.get(flow.refFlowPropertyId), prop -> {
				if (prop == null)
					return;
				ref.property = Descriptor.of(prop);
				if (prop.unitGroup != null
					&& prop.unitGroup.referenceUnit != null) {
					ref.unit = Descriptor.of(prop.unitGroup.referenceUnit);
				}
			});
			refs.add(ref);
		});
		return refs;
	}

	@Override
	public void sync(Stream<FlowRef> externalRefs) {
		if (externalRefs == null)
			return;
		externalRefs.forEach(this::sync);
	}

	@Override
	public void persist(List<FlowRef> refs, IDatabase db) {
	}

	/**
	 * Sync the flow references with the respective flow in the database (when
	 * it exists). It tests that the definition of the flow reference can be
	 * fulfilled with a database flow (i.e. the ref-IDs match and the flow
	 * property and unit is defined for that flow). If this is not the case it
	 * returns it sets an error state to the given reference. Otherwise, it will
	 * mutate the flow reference to have the respective database IDs of the
	 * corresponding flow and sets the property and unit to the respective
	 * defaults if they are missing. Also, it will return the matching flow in
	 * case there was no error (otherwise null).
	 */
	public Flow sync(FlowRef ref) {
		if (Sync.isInvalidFlowRef(ref))
			return null;

		// we update the status in the following sync. steps
		ref.status = null;

		// check the flow
		Flow flow = new FlowDao(db).getForRefId(ref.flow.refId);
		if (flow == null) {
			ref.status = MappingStatus.error("there is no flow with id="
				+ ref.flow.refId + " in the database");
			return null;
		}

		// check the flow property
		FlowProperty prop = null;
		if (ref.property == null) {
			prop = flow.referenceFlowProperty;
		} else {
			for (FlowPropertyFactor f : flow.flowPropertyFactors) {
				if (f.flowProperty == null)
					continue;
				if (Objects.equals(
					ref.property.refId, f.flowProperty.refId)) {
					prop = f.flowProperty;
					break;
				}
			}
		}
		if (prop == null || prop.unitGroup == null) {
			ref.status = MappingStatus.error("the flow in the database has"
				+ " no corresponding flow property");
			return null;
		}

		// check the unit
		Unit u = null;
		if (ref.unit == null) {
			u = prop.unitGroup.referenceUnit;
		} else {
			for (Unit ui : prop.unitGroup.units) {
				if (Objects.equals(ref.unit.refId, ui.refId)) {
					u = ui;
					break;
				}
			}
		}
		if (u == null) {
			ref.status = MappingStatus.error("the flow in the database has"
				+ " no corresponding unit");
			return null;
		}

		// check a possible provider
		Process provider = null;
		if (ref.provider != null) {
			provider = new ProcessDao(db).getForRefId(ref.provider.refId);
			if (provider == null) {
				ref.status = MappingStatus.error(
					"the provider does not exist in the database");
				return null;
			}
			boolean exists = provider.exchanges.stream().anyMatch(
				e -> !e.isAvoided
					&& Objects.equals(e.flow, flow)
					&& ((e.isInput && flow.flowType == FlowType.WASTE_FLOW)
					|| (!e.isInput && flow.flowType == FlowType.PRODUCT_FLOW)));
			if (!exists) {
				ref.status = MappingStatus.error(
					"the given provider does not deliver that flow");
				return null;
			}
		}

		// sync the reference data
		if (ref.property == null) {
			ref.property = Descriptor.of(prop);
		}
		if (ref.unit == null) {
			ref.unit = Descriptor.of(u);
		}
		ref.flow.id = flow.id;
		ref.property.id = prop.id;
		ref.unit.id = u.id;

		if (provider != null) {
			ref.provider = Descriptor.of(provider);
		}

		Sync.checkFlowName(ref, flow.name);
		Sync.checkFlowCategory(ref,
			String.join("/", Categories.path(flow.category)));
		Sync.checkFlowType(ref, flow.flowType);
		Sync.checkFlowLocation(ref, flow.location == null
			? null
			: flow.location.code);
		if (provider != null) {
			Sync.checkProviderLocation(ref, provider.location == null
				? null
				: provider.location.code);
		}

		if (ref.status == null) {
			ref.status = MappingStatus.ok("flow in sync. with database");
		}
		return flow;
	}
}
