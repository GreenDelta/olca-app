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
import org.openlca.simapro.csv.enums.ProductType;
import org.openlca.simapro.csv.enums.SubCompartment;
import org.openlca.simapro.csv.process.ProcessBlock;
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
		var quantities = Quantities.of(dataSet);

		refs = new ArrayList<>();
		var handled = new HashSet<String>();
		Consumer<FlowInfo> handle = info -> {
			var key = info.key();
			if (handled.contains(key))
				return;
			var flowRef = info.flowRef();
			refs.add(flowRef);
			handled.add(key);
		};

		// process flows
		for (var process : dataSet.processes()) {

			// product outputs
			for (var output : process.products()) {
				var type = productTypeOf(process);
				var info = new FlowInfo(type, quantities)
					.name(output.name())
					.unit(output.unit());
				handle.accept(info);
			}

			// product inputs
			for (var techType : ProductType.values()) {
				for (var exchange : process.exchangesOf(techType)) {
					var flowType = techType == ProductType.WASTE_TO_TREATMENT
						? FlowType.WASTE_FLOW
						: FlowType.PRODUCT_FLOW;
					var info = new FlowInfo(flowType, quantities)
						.name(exchange.name())
						.unit(exchange.unit());
					handle.accept(info);
				}
			}

			// add elementary flows
			for (var elemType : ElementaryFlowType.values()) {
				for (var exchange : process.exchangesOf(elemType)) {
					var info = FlowInfo.of(elemType, quantities)
						.subCompartment(exchange.subCompartment())
						.name(exchange.name())
						.unit(exchange.unit());
					handle.accept(info);
				}
			}
		}

		for (var method : dataSet.methods()) {
			for (var impact : method.impactCategories()) {
				for (var factor : impact.factors()) {
					var type = ElementaryFlowType.of(factor.compartment());
					if (type == null)
						continue;
					var info = FlowInfo.of(type, quantities)
						.subCompartment(factor.subCompartment())
						.name(factor.flow())
						.unit(factor.unit());
					handle.accept(info);
				}
			}
		}

		return refs;
	}

	private FlowType productTypeOf(ProcessBlock block) {
		if (block.category() == null)
			return FlowType.PRODUCT_FLOW;
		return switch (block.category()) {
			case WASTE_SCENARIO, WASTE_TREATMENT -> FlowType.WASTE_FLOW;
			default -> FlowType.PRODUCT_FLOW;
		};
	}

	@Override
	public void persist(List<FlowRef> refs, IDatabase db) {
	}

	@Override
	public void sync(Stream<FlowRef> externalRefs) {
		Sync.packageSync(this, externalRefs);
	}

	private record Quantities(
		Map<String, UnitDescriptor> units,
		Map<String, FlowPropertyDescriptor> properties) {

		static Quantities of(CsvDataSet dataSet) {

			var props = new HashMap<String, FlowPropertyDescriptor>();
			for (var quantity : dataSet.quantities()) {
				var d = new FlowPropertyDescriptor();
				d.name = quantity.name();
				props.put(quantity.name(), d);
			}

			var units = new HashMap<String, UnitDescriptor>();
			var unitProps = new HashMap<String, FlowPropertyDescriptor>();
			for (var unit : dataSet.units()) {
				var d = new UnitDescriptor();
				d.name = unit.name();
				units.put(unit.name(), d);
				var prop = props.get(unit.quantity());
				unitProps.put(unit.name(), prop);
			}

			return new Quantities(units, unitProps);
		}

		FlowPropertyDescriptor propertyOf(String unit) {
			return properties.get(unit);
		}

		UnitDescriptor unitOf(String unit) {
			return units.get(unit);
		}

	}

	private static class FlowInfo {

		final FlowType flowType;
		final Quantities quantities;
		String compartment;
		String subCompartment;
		String name;
		String unit;

		FlowInfo(FlowType flowType, Quantities quantities) {
			this.flowType = flowType;
			this.quantities = quantities;
		}

		static FlowInfo of(ElementaryFlowType type, Quantities quantities) {
			var info = new FlowInfo(FlowType.ELEMENTARY_FLOW, quantities);
			info.compartment = type.exchangeHeader();
			return info;
		}

		FlowInfo subCompartment(String sub) {
			var subComp = SubCompartment.of(sub);
			subCompartment = subComp == null
				? SubCompartment.UNSPECIFIED.toString()
				: subComp.toString();
			return this;
		}

		FlowInfo name(String name) {
			this.name = Strings.orEmpty(name).trim();
			return this;
		}

		FlowInfo unit(String unit) {
			this.unit = Strings.orEmpty(unit).trim();
			return this;
		}

		String key() {
			return switch (flowType) {
				case PRODUCT_FLOW ->  KeyGen.toPath("product", name, unit);
				case WASTE_FLOW ->  KeyGen.toPath("waste", name, unit);
				case ELEMENTARY_FLOW -> KeyGen.toPath(
					"elementary flow", compartment, subCompartment, name, unit);
			};
		}

		FlowRef flowRef() {
			var flowRef = new FlowRef();
			var flow = new FlowDescriptor();
			flow.flowType = flowType;
			flow.name = name;
			flow.refId = key();
			flowRef.flow = flow;
			if (flowType == FlowType.ELEMENTARY_FLOW) {
				flowRef.flowCategory = compartment + "/" + subCompartment;
			}
			flowRef.unit = quantities.unitOf(unit);
			flowRef.property = quantities.propertyOf(unit);
			return flowRef;
		}

	}
}
