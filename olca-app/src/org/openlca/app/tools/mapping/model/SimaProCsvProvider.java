package org.openlca.app.tools.mapping.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;
import org.openlca.core.model.descriptors.UnitDescriptor;
import org.openlca.io.maps.FlowRef;
import org.openlca.simapro.csv.CsvDataSet;
import org.openlca.simapro.csv.SimaProCsv;
import org.openlca.simapro.csv.enums.ElementaryFlowType;
import org.openlca.simapro.csv.enums.SubCompartment;
import org.openlca.util.KeyGen;
import org.openlca.util.Strings;

public class SimaProCsvProvider implements IProvider {

	private final File file;
	private List<FlowRef> refs;

	private SimaProCsvProvider(File file) {
		this.file = file;
	}

	public static SimaProCsvProvider of(File file) {
		return new SimaProCsvProvider(file);
	}

	public File file() {
		return file;
	}

	@Override
	public List<FlowRef> getFlowRefs() {
		if (refs != null)
			return refs;

		var dataSet = SimaProCsv.read(file);
		var quantities = QuantityDescriptors.of(dataSet);

		refs = new ArrayList<>();
		var handled = new HashSet<String>();
		Consumer<Path> handle = path -> {
			var key = path.key();
			if (handled.contains(key))
				return;
			var flowRef = path.flowRef(quantities);
			refs.add(flowRef);
			handled.add(key);
		};

		for (var process : dataSet.processes()) {
			for (var flowType : ElementaryFlowType.values()) {
				for (var exchange : process.exchangesOf(flowType)) {
					var path = Path.of(flowType)
						.subCompartment(exchange.subCompartment())
						.name(exchange.name())
						.unit(exchange.unit());
					handle.accept(path);
				}
			}
		}

		for (var method : dataSet.methods()) {
			for (var impact : method.impactCategories()) {
				for (var factor : impact.factors()) {
					var type = ElementaryFlowType.of(factor.compartment());
					if (type == null)
						continue;
					var path = Path.of(type)
						.subCompartment(factor.subCompartment())
						.name(factor.flow())
						.unit(factor.unit());
					handle.accept(path);
				}
			}
		}

		return refs;
	}

	@Override
	public void persist(List<FlowRef> refs, IDatabase db) {

	}

	@Override
	public void sync(Stream<FlowRef> externalRefs) {
		Sync.packageSync(this, externalRefs);
	}

	private record QuantityDescriptors(
		Map<String, UnitDescriptor> units,
		Map<String, FlowPropertyDescriptor> properties) {

		static QuantityDescriptors of(CsvDataSet dataSet) {

			var props = new HashMap<String, FlowPropertyDescriptor>();
			for (var quantity : dataSet.quantities()) {
				var d = new FlowPropertyDescriptor();
				d.name = quantity.name();
				props.put(quantity.name(), d);
			}

			var units = new HashMap<String, UnitDescriptor> ();
			var unitProps = new HashMap<String, FlowPropertyDescriptor>();
			for (var unit : dataSet.units()) {
				var d = new UnitDescriptor();
				d.name = unit.name();
				units.put(unit.name(), d);
				var prop = props.get(unit.quantity());
				unitProps.put(unit.name(), prop);
			}

			return new QuantityDescriptors(units, unitProps);
		}

		FlowPropertyDescriptor propertyOf(String unit) {
			return properties.get(unit);
		}

		UnitDescriptor unitOf(String unit) {
			return units.get(unit);
		}

	}

	private record Path(String[] slots) {

		static Path of(ElementaryFlowType type) {
			var slots = new String[4];
			slots[0] = type.compartment();
			return new Path(slots);
		}

		Path subCompartment(String sub) {
			var subComp = SubCompartment.of(sub);
			slots[1] = subComp == null
				? SubCompartment.UNSPECIFIED.toString()
				: subComp.toString();
			return this;
		}

		Path name(String name) {
			slots[2] = Strings.orEmpty(name).trim();
			return this;
		}

		Path unit(String unit) {
			slots[3] = Strings.orEmpty(unit).trim();
			return this;
		}

		String key() {
			return KeyGen.toPath(slots);
		}

		FlowRef flowRef(QuantityDescriptors quantities) {
			var flowRef = new FlowRef();
			var flow = new FlowDescriptor();
			flow.flowType = FlowType.ELEMENTARY_FLOW;
			flow.name = slots[2];
			flow.refId = key();
			flowRef.flow = flow;
			flowRef.flowCategory = KeyGen.toPath(slots[0], slots[1]);
			flowRef.unit = quantities.unitOf(slots[3]);
			flowRef.property = quantities.propertyOf(slots[3]);
			return flowRef;
		}

	}
}
