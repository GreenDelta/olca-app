package org.openlca.app.results.impacts;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.LcaResult;

/**
 * Describes an item in the impact tree. The tree has 3 levels: roots, inner
 * nodes, and leafs. A root contains the total impact result. The inner nodes
 * and leafs contain the contributions of technosphere flows and intervention
 * flows to that impact result.
 */
record Item(
		LcaResult result,
		ImpactDescriptor impact,
		Item parent,
		TechFlow techFlow,
		EnviFlow enviFlow,
		double impactResult,
		double inventoryResult) {

	private static Item rootOf(LcaResult result, ImpactDescriptor impact) {
		double value = result.getTotalImpactValueOf(impact);
		return new Item(result, impact, null, null, null, value, 0);
	}

	private static Item techNodeOf(Item root, TechFlow techFlow) {
		var result = root.result;
		double value = result.getDirectImpactOf(root.impact, techFlow);
		return new Item(result, root.impact, root, techFlow, null, value, 0);
	}

	private static Item enviNodeOf(Item parent, EnviFlow enviFlow) {
		var result = parent.result;
		var impact = parent.impact;
		double impactVal = result.getFlowImpactOf(impact, enviFlow);
		double inventoryVal = result.getTotalFlowValueOf(enviFlow);
		return new Item(
				result, impact, parent, null, enviFlow, impactVal, inventoryVal);
	}

	private static Item techLeafOf(Item parent, TechFlow techFlow) {
		var result = parent.result;
		var enviFlow = parent.enviFlow;
		double inventoryVal = result.getDirectFlowOf(enviFlow, techFlow);
		double impactVal = parent.impactFactor() * inventoryVal;
		return new Item(
				result, parent.impact, parent, techFlow, null, impactVal, inventoryVal);
	}

	private static Item enviLeafOf(Item parent, EnviFlow enviFlow) {
		var result = parent.result;
		var impact = parent.impact;
		var impactFactor = result.getImpactFactorOf(impact, enviFlow);
		var inventoryVal = result.getDirectFlowOf(enviFlow, parent.techFlow);
		double impactVal = impactFactor * inventoryVal;
		return new Item(
				result, parent.impact, parent, null, enviFlow, impactVal, inventoryVal);
	}

	static List<Item> rootsOf(LcaResult result, List<ImpactDescriptor> impacts) {
		return impacts.stream()
				.map(impact -> rootOf(result, impact))
				.toList();
	}

	List<Item> techNodesOf(List<TechFlow> techFlows, double cutoff) {
		return isRoot()
				? collect(techFlows, cutoff, Item::techNodeOf)
				: Collections.emptyList();
	}

	List<Item> techLeafsOf(List<TechFlow> techFlows, double cutoff) {
		return isEnviItem()
				? collect(techFlows, cutoff, Item::techLeafOf)
				: Collections.emptyList();
	}

	List<Item> enviNodesOf(List<EnviFlow> enviFlows, double cutoff) {
		return isRoot()
				? collect(enviFlows, cutoff, Item::enviNodeOf)
				: Collections.emptyList();
	}

	List<Item> enviLeafsOf(List<EnviFlow> enviFlows, double cutoff) {
		return isTechItem()
				? collect(enviFlows, cutoff, Item::enviLeafOf)
				: Collections.emptyList();
	}

	private <E> List<Item> collect(
			List<E> elements, double cutoff, BiFunction<Item, E, Item> fn) {
		var cutoffFilter = cutoffFilterOf(cutoff);
		return elements.stream()
				.map(e -> fn.apply(this, e))
				.filter(cutoffFilter)
				.sorted((i1, i2) -> Double.compare(i2.impactResult, i1.impactResult))
				.toList();
	}

	private Predicate<Item> cutoffFilterOf(double cutoff) {
		if (cutoff == 0)
			return item -> true;
		double absMax = Math.abs(result.getTotalImpactValueOf(impact) * cutoff);
		return item -> Math.abs(item.impactResult) > absMax;
	}


	boolean isRoot() {
		return parent == null;
	}

	boolean isInnerNode() {
		return parent != null && parent().isRoot();
	}

	boolean isLeaf() {
		return parent != null && parent.isInnerNode();
	}

	boolean isEnviItem() {
		return enviFlow != null;
	}

	boolean isTechItem() {
		return techFlow != null;
	}

	double impactFactor() {
		if (isRoot())
			return 0;
		return isEnviItem()
				? result.getImpactFactorOf(impact, enviFlow)
				: parent.impactFactor();
	}

}
