package org.openlca.app.tools.mapping.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openlca.app.util.Fn;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.FlowPropertyDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.LocationDao;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.io.maps.FlowMap;
import org.openlca.io.maps.FlowRef;
import org.openlca.util.CategoryPathBuilder;

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
		// TODO: not yet implemented
	}

	@Override
	public void syncTargetFlows(FlowMap fm) {
		// TODO: not yet implemented
	}

	@Override
	public void persist(List<FlowRef> refs, IDatabase db) {
	}
}
