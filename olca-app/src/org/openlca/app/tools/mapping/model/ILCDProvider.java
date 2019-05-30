package org.openlca.app.tools.mapping.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.openlca.io.maps.FlowMap;
import org.openlca.io.maps.FlowRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ILCDProvider implements IProvider {

	public final File file;

	private ILCDProvider(File file) {
		this.file = file;
	}

	public static ILCDProvider of(String path) {
		return new ILCDProvider(new File(path));
	}

	public static ILCDProvider of(File file) {
		return new ILCDProvider(file);
	}

	@Override
	public List<FlowRef> getFlowRefs() {
		List<FlowRef> refs = new ArrayList<>();
		try (ZipStore store = new ZipStore(file)) {

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
				flowRef.flowCategory = String.join("/", cpath);

				// flow property & unit
				FlowPropertyRef refProp = Flows.getReferenceFlowProperty(f);
				if (refProp == null || refProp.flowProperty == null)
					return;
				String propID = refProp.flowProperty.uuid;
				flowRef.property = props.get(propID);
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
	public void persist(List<FlowRef> refs, IDatabase db) {
		if (refs == null || db == null)
			return;
		try (ZipStore store = new ZipStore(file)) {
			FlowDao dao = new FlowDao(db);
			ImportConfig conf = new ImportConfig(store, db);
			for (FlowRef ref : refs) {
				Flow flow = dao.getForRefId(ref.flow.refId);
				if (flow != null)
					continue;
				FlowImport imp = new FlowImport(conf);
				flow = imp.run(ref.flow.refId);
			}
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed persist flows", e);
		}
	}

	@Override
	public void syncSourceFlows(FlowMap fm) {
		// TODO: not yet implemented
	}

	@Override
	public void syncTargetFlows(FlowMap fm) {
		// TODO: not yet implemented
	}

}
