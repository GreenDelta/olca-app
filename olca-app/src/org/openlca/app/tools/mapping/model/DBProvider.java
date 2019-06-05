package org.openlca.app.tools.mapping.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openlca.app.util.Fn;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.FlowPropertyDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.LocationDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.io.maps.FlowMap;
import org.openlca.io.maps.FlowMapEntry;
import org.openlca.io.maps.FlowRef;
import org.openlca.io.maps.Status;
import org.openlca.util.Categories;
import org.openlca.util.CategoryPathBuilder;
import org.openlca.util.Strings;

public class DBProvider implements IProvider {

	public final IDatabase db;

	public DBProvider(IDatabase db) {
		this.db = db;
	}

	@Override
	public List<FlowRef> getFlowRefs() {

		// collect categories, properties, locations
		CategoryPathBuilder categories = new CategoryPathBuilder(db);
		Map<Long, FlowProperty> props = new FlowPropertyDao(db)
				.getAll().stream()
				.collect(Collectors.toMap(fp -> fp.id, fp -> fp));
		Map<Long, String> locations = new LocationDao(db).getCodes();

		List<FlowRef> refs = new ArrayList<FlowRef>();
		new FlowDao(db).getDescriptors().stream().forEach(flow -> {
			FlowRef ref = new FlowRef();
			ref.flow = flow;
			ref.flowCategory = categories.build(flow.category);
			ref.flowLocation = locations.get(flow.location);
			Fn.with(props.get(flow.refFlowPropertyId), prop -> {
				if (prop == null)
					return;
				ref.property = Descriptors.toDescriptor(prop);
				if (prop.unitGroup != null
						&& prop.unitGroup.referenceUnit != null) {
					ref.unit = Descriptors.toDescriptor(
							prop.unitGroup.referenceUnit);
				}
			});
			refs.add(ref);
		});
		return refs;
	}

	@Override
	public void syncSourceFlows(FlowMap fm) {
		if (fm == null)
			return;
		for (FlowMapEntry e : fm.entries) {
			sync(e.sourceFlow);
		}
	}

	@Override
	public void syncTargetFlows(FlowMap fm) {
		if (fm == null)
			return;
		for (FlowMapEntry e : fm.entries) {
			sync(e.targetFlow);
		}
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
		if (ref == null)
			return null;
		if (ref.flow == null || ref.flow.refId == null) {
			ref.status = Status.error("missing flow reference with UUID");
			return null;
		}

		// we update the status in the following sync. steps
		ref.status = null;

		// check the flow
		Flow flow = new FlowDao(db).getForRefId(ref.flow.refId);
		if (flow == null) {
			ref.status = Status.error("there is no flow with id="
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
			ref.status = Status.error("the flow in the database has"
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
			ref.status = Status.error("the flow in the database has"
					+ " no corresponding unit");
			return null;
		}

		// TODO: sync the provider

		// sync the reference data
		if (ref.property == null) {
			ref.property = Descriptors.toDescriptor(prop);
		}
		if (ref.unit == null) {
			ref.unit = Descriptors.toDescriptor(u);
		}
		ref.flow.id = flow.id;
		ref.property.id = prop.id;
		ref.unit.id = u.id;

		// check the category
		String catpath = String.join("/", Categories.path(flow.category));
		if (Strings.nullOrEmpty(ref.flowCategory)) {
			ref.flowCategory = catpath;
		} else if (!Strings.nullOrEqual(catpath, ref.flowCategory)) {
			ref.status = Status.warn(
					"the flow in the database has a different category path");
		}

		// TODO: check the flow location

		if (ref.status == null) {
			ref.status = Status.ok("flow in sync. with database");
		}
		return flow;
	}
}
