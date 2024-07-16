package org.openlca.app.tools.mapping.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.openlca.core.database.IDatabase;
import org.openlca.core.io.maps.FlowRef;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;
import org.openlca.core.model.descriptors.UnitDescriptor;
import org.openlca.ilcd.commons.LangString;
import org.openlca.ilcd.commons.Ref;
import org.openlca.ilcd.flowproperties.FlowProperty;
import org.openlca.ilcd.io.ZipStore;
import org.openlca.ilcd.units.UnitGroup;
import org.openlca.ilcd.util.Categories;
import org.openlca.ilcd.util.FlowProperties;
import org.openlca.ilcd.util.Flows;
import org.openlca.ilcd.util.UnitGroups;
import org.openlca.io.ilcd.input.FlowImport;
import org.openlca.io.ilcd.input.Import;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ILCDProvider implements FlowProvider {

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
		try (var store = new ZipStore(file)) {

			// collect units
			var units = new HashMap<String, Descriptor>();
			for (var ug : store.iter(UnitGroup.class)) {
				var unit = UnitGroups.getReferenceUnit(ug);
				if (unit == null)
					continue;
				var d = new UnitDescriptor();
				d.name = unit.getName();
				units.put(UnitGroups.getUUID(ug), d);
			}

			// collect flow properties
			var props = new HashMap<String, Descriptor>();
			for (var fp : store.iter(FlowProperty.class)) {
				var d = new FlowPropertyDescriptor();
				d.refId = FlowProperties.getUUID(fp);
				d.name = LangString.getDefault(FlowProperties.getName(fp));
				props.put(d.refId, d);
				Ref ug = FlowProperties.getUnitGroupRef(fp);
				if (ug != null) {
					Descriptor unit = units.get(ug.getUUID());
					units.put(d.refId, unit);
				}
			}

			// collect flows
			for (var f : store.iter(org.openlca.ilcd.flows.Flow.class)) {
				var flowRef = new FlowRef();
				refs.add(flowRef);

				// flow
				var d = new FlowDescriptor();
				flowRef.flow = d;
				d.name = Flows.getFullName(f, "en");
				d.flowType = map(Flows.getFlowType(f));
				d.refId = Flows.getUUID(f);

				// category path
				String[] cpath = Categories.getPath(f);
				flowRef.flowCategory = String.join("/", cpath);

				// flow property & unit
				var refProp = Flows.getReferenceFlowProperty(f);
				if (refProp == null || refProp.getFlowProperty() == null)
					continue;
				String propID = refProp.getFlowProperty().getUUID();
				flowRef.property = props.get(propID);
				flowRef.unit = units.get(propID);
			}

		} catch (Exception e) {
			var log = LoggerFactory.getLogger(getClass());
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
			var imp = Import.of(store, db);
			for (FlowRef ref : refs) {
				FlowImport.get(imp, ref.flow.refId);
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
