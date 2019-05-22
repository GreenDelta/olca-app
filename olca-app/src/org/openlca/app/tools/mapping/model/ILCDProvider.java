package org.openlca.app.tools.mapping.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;
import org.openlca.core.model.descriptors.UnitDescriptor;
import org.openlca.ilcd.commons.LangString;
import org.openlca.ilcd.commons.Ref;
import org.openlca.ilcd.flowproperties.FlowProperty;
import org.openlca.ilcd.flows.FlowPropertyRef;
import org.openlca.ilcd.io.ZipStore;
import org.openlca.ilcd.units.Unit;
import org.openlca.ilcd.units.UnitGroup;
import org.openlca.ilcd.util.Categories;
import org.openlca.ilcd.util.FlowProperties;
import org.openlca.ilcd.util.Flows;
import org.openlca.ilcd.util.UnitGroups;
import org.openlca.io.ilcd.input.FlowImport;
import org.openlca.io.ilcd.input.ImportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ILCDProvider implements IMapProvider {

	public final ZipStore store;

	public ILCDProvider(File file) {
		try {
			store = new ZipStore(file);
		} catch (Exception e) {
			throw new RuntimeException("Could not open zip", e);
		}
	}

	@Override
	public List<FlowRef> getFlowRefs() {
		List<FlowRef> refs = new ArrayList<>();
		try {

			// collect units
			Map<String, BaseDescriptor> units = new HashMap<>();
			store.each(UnitGroup.class, ug -> {
				Unit unit = UnitGroups.getReferenceUnit(ug);
				if (unit == null)
					return;
				BaseDescriptor d = new UnitDescriptor();
				d.name = unit.name;
				d.description = LangString.getFirst(unit.comment, "en");
				units.put(ug.getUUID(), d);
			});

			// collect flow properties
			Map<String, BaseDescriptor> props = new HashMap<>();
			store.each(FlowProperty.class, fp -> {
				BaseDescriptor d = new FlowPropertyDescriptor();
				d.refId = fp.getUUID();
				d.name = LangString.getFirst(fp.getName(), "en");
				props.put(d.refId, d);
				Ref ug = FlowProperties.getUnitGroupRef(fp);
				if (ug != null) {
					BaseDescriptor unit = units.get(ug.uuid);
					units.put(d.refId, unit);
				}
			});

			// collect flows
			store.each(org.openlca.ilcd.flows.Flow.class, f -> {
				FlowRef flowRef = new FlowRef();
				refs.add(flowRef);

				// flow
				FlowDescriptor d = new FlowDescriptor();
				flowRef.flow = d;
				d.name = Flows.getFullName(f);
				d.flowType = map(Flows.getType(f));
				d.refId = f.getUUID();

				// category path
				String[] cpath = Categories.getPath(f);
				flowRef.categoryPath = String.join("/", cpath);

				// flow property & unit
				FlowPropertyRef refProp = Flows.getReferenceFlowProperty(f);
				if (refProp == null || refProp.flowProperty == null)
					return;
				String propID = refProp.flowProperty.uuid;
				flowRef.flowProperty = props.get(propID);
				flowRef.unit = units.get(propID);
			});

		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to get flow refs", e);
		}

		return refs;
	}

	private FlowType map(org.openlca.ilcd.commons.FlowType t) {
		if (t == null)
			return FlowType.ELEMENTARY_FLOW;
		switch (t) {
		case ELEMENTARY_FLOW:
			return FlowType.ELEMENTARY_FLOW;
		case PRODUCT_FLOW:
			return FlowType.PRODUCT_FLOW;
		case WASTE_FLOW:
			return FlowType.WASTE_FLOW;
		default:
			return FlowType.ELEMENTARY_FLOW;
		}
	}

	@Override
	public Optional<Flow> persist(FlowRef ref, IDatabase db) {
		if (ref == null || ref.flow == null || db == null)
			return Optional.empty();
		FlowDao dao = new FlowDao(db);
		Flow flow = dao.getForRefId(ref.flow.refId);
		if (flow != null)
			return Optional.of(flow);
		try {
			ImportConfig conf = new ImportConfig(store, db);
			FlowImport imp = new FlowImport(conf);
			flow = imp.run(ref.flow.refId);
			return Optional.ofNullable(flow);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to import flow " + ref.flow, e);
			return Optional.empty();
		}
	}

	@Override
	public void close() throws IOException {
		try {
			store.close();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to close ILCD zip store", e);
		}
	}
}
