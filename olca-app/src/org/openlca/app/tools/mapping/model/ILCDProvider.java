package org.openlca.app.tools.mapping.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.Descriptor;
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
import org.openlca.io.maps.FlowRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ILCDProvider implements IProvider {

	private final File file;
	private List<FlowRef> refs;

	private ILCDProvider(File file) {
		this.file = file;
	}

	public static ILCDProvider of(String path) {
		return new ILCDProvider(new File(path));
	}

	public static ILCDProvider of(File file) {
		return new ILCDProvider(file);
	}

	public File file() {
		return file;
	}

	@Override
	public List<FlowRef> getFlowRefs() {
		if (refs != null)
			return refs;
		refs = new ArrayList<>();
		try (ZipStore store = new ZipStore(file)) {

			// collect units
			Map<String, Descriptor> units = new HashMap<>();
			store.each(UnitGroup.class, ug -> {
				Unit unit = UnitGroups.getReferenceUnit(ug);
				if (unit == null)
					return;
				Descriptor d = new UnitDescriptor();
				d.name = unit.name;
				d.description = LangString.getFirst(unit.comment, "en");
				units.put(ug.getUUID(), d);
			});

			// collect flow properties
			Map<String, Descriptor> props = new HashMap<>();
			store.each(FlowProperty.class, fp -> {
				Descriptor d = new FlowPropertyDescriptor();
				d.refId = fp.getUUID();
				d.name = LangString.getFirst(fp.getName(), "en");
				props.put(d.refId, d);
				Ref ug = FlowProperties.getUnitGroupRef(fp);
				if (ug != null) {
					Descriptor unit = units.get(ug.uuid);
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
		return switch (t) {
			case PRODUCT_FLOW -> FlowType.PRODUCT_FLOW;
			case WASTE_FLOW -> FlowType.WASTE_FLOW;
			default -> FlowType.ELEMENTARY_FLOW;
		};
	}

	@Override
	public void persist(List<FlowRef> refs, IDatabase db) {
		if (refs == null || db == null)
			return;
		try (var store = new ZipStore(file)) {
			var conf = new ImportConfig(store, db);
			for (FlowRef ref : refs) {
				FlowImport.get(conf, ref.flow.refId);
			}
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed persist flows", e);
		}
	}

	@Override
	public void sync(Stream<FlowRef> externalRefs) {
		Sync.packageSync(this, externalRefs);
	}
}
