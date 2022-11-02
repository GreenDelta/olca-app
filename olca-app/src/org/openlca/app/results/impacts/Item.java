package org.openlca.app.results.impacts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.LcaResult;

class Item {

	private final LcaResult result;
	private final ImpactDescriptor impact;
	private final Item parent;

	private final TechFlow techFlow;
	private final EnviFlow enviFlow;

	private double impactResult;
	private double inventoryResult;

	private Item(LcaResult result, ImpactDescriptor impact) {
		this.result = result;
		this.impact = impact;
		this.parent = null;
		this.techFlow = null;
		this.enviFlow = null;
	}

	private Item(Item parent, TechFlow techFlow) {
		this.parent = parent;
		this.result = parent.result;
		this.techFlow = techFlow;
		this.impact = null;
		this.enviFlow = null;
	}

	private Item(Item parent, EnviFlow enviFlow) {
		this.parent = parent;
		this.result = parent.result;
		this.impact = null;
		this.techFlow = null;
		this.enviFlow = enviFlow;
	}

	static List<Item> rootsOf(LcaResult result, List<ImpactDescriptor> impacts) {
		var items = new ArrayList<Item>(impacts.size());
		for (var impact : impacts) {
			var item = new Item(result, impact);
			item.impactResult = result.getTotalImpactValueOf(impact);
			items.add(item);
		}
		return items;
	}

	boolean isRoot() {
		return impact != null;
	}

	ImpactDescriptor impact() {
		return parent != null
				? parent.impact()
				: impact;
	}

	private LcaResult result() {
		return parent != null
				? parent.result
				: result;
	}

	boolean isEnviItem() {
		return enviFlow != null;
	}

	EnviFlow enviFlow() {
		return enviFlow != null
				? enviFlow
				: parent != null ? parent.enviFlow : null;
	}

	boolean isTechItem() {
		return techFlow != null;
	}

	TechFlow techFlow() {
		return techFlow != null
				? techFlow
				: parent != null ? parent.techFlow : null;
	}

	boolean isLeaf() {
		return techFlow != null && enviFlow != null;
	}

	double impactFactor() {
		var enviFlow = enviFlow();
		return enviFlow != null
				? result.getImpactFactorOf(impact(), enviFlow)
				: 0;
	}

	List<Item> techNodesOf(List<TechFlow> techFlows, double cutoff) {
		var collector = new Collector(this, cutoff);
		for (var techFlow : techFlows) {
			double value = result.getDirectImpactOf(impact(), techFlow);
			collector.next(value, () -> new Item(this, techFlow));
		}
		return collector.get();
	}

	List<Item> enviNodesOf(List<EnviFlow> enviFlows, double cutoff) {
		var collector = new Collector(this, cutoff);
		for (var enviFlow : enviFlows) {
			double value = result.getFlowImpactOf(impact(), enviFlow);
			collector.next(value, () -> {
				var item = new Item(this, enviFlow);
				item.inventoryResult = result.getTotalFlowValueOf(enviFlow);
				return item;
			});
		}
		return collector.get();
	}

	List<Item> enviLeafsOf(List<EnviFlow> enviFlows, double cutoff) {
		if (!isTechItem())
			return Collections.emptyList();
		var collector = new Collector(this, cutoff);
		for (var enviFlow : enviFlows) {
			double factor = result.getImpactFactorOf(impact(), enviFlow);
			double inventoryResult = result.getDirectFlowOf(enviFlow, this.techFlow);
			double value = factor * inventoryResult;
			collector.next(value, () -> {
				var item = new Item(this, enviFlow);
				item.inventoryResult = inventoryResult;
				return item;
			});
		}
		return collector.get();
	}

	List<Item> techLeafsOf(List<TechFlow> techFlows, double cutoff) {
		if (!isEnviItem())
			return Collections.emptyList();
		var collector = new Collector(this, cutoff);
		var factor = impactFactor();
		for (var techFlow : techFlows) {
			var inventoryResult = result.getDirectFlowOf(this.enviFlow, techFlow);
			double value = factor * inventoryResult;
			collector.next(value, () -> {
				var item = new Item(this, techFlow);
				item.inventoryResult = inventoryResult;
				return item;
			});
		}
		return collector.get();
	}

	private static class Collector {

		private final boolean addAll;
		private final double absMax;
		private final List<Item> items = new ArrayList<>();

		Collector(Item parent, double cutoff) {
			if (cutoff == 0) {
				addAll = true;
				absMax = 0;
			} else {
				double total = parent.result.getTotalImpactValueOf(parent.impact());
				addAll = false;
				absMax = Math.abs(cutoff * total);
			}
		}

		void next(double value, Supplier<Item> fn) {
			if (!addAll && Math.abs(value) < absMax)
				return;
			var item = fn.get();
			item.impactResult = value;
			items.add(item);
		}

		List<Item> get() {
			items.sort((i1, i2) -> Double.compare(i2.impactResult, i1.impactResult));
			return items;
		}
	}
}
