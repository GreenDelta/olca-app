package org.openlca.app.results.impacts;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.LcaResult;

/**
 * Describes an item in the impact tree. The tree has 3 levels: roots, inner
 * nodes, and leafs. A root contains the total impact result. The inner nodes
 * and leafs contain the contributions of technosphere flows and intervention
 * flows to that impact result.
 */
record TreeItem(
		LcaResult result,
		ImpactDescriptor impact,
		TreeItem parent,
		TechFlow techFlow,
		EnviFlow enviFlow,
		double impactResult,
		double inventoryResult) {

	private static TreeItem rootOf(LcaResult result, ImpactDescriptor impact) {
		double value = result.getTotalImpactValueOf(impact);
		return new TreeItem(result, impact, null, null, null, value, 0);
	}

	private static TreeItem techNodeOf(TreeItem root, TechFlow techFlow) {
		var result = root.result;
		double value = result.getDirectImpactOf(root.impact, techFlow);
		return new TreeItem(result, root.impact, root, techFlow, null, value, 0);
	}

	private static TreeItem enviNodeOf(TreeItem parent, EnviFlow enviFlow) {
		var result = parent.result;
		var impact = parent.impact;
		double impactVal = result.getFlowImpactOf(impact, enviFlow);
		double inventoryVal = result.getTotalFlowValueOf(enviFlow);
		return new TreeItem(
				result, impact, parent, null, enviFlow, impactVal, inventoryVal);
	}

	private static TreeItem techLeafOf(TreeItem parent, TechFlow techFlow) {
		var result = parent.result;
		var enviFlow = parent.enviFlow;
		double inventoryVal = result.getDirectFlowOf(enviFlow, techFlow);
		double impactVal = parent.impactFactor() * inventoryVal;
		return new TreeItem(
				result, parent.impact, parent, techFlow, null, impactVal, inventoryVal);
	}

	private static TreeItem enviLeafOf(TreeItem parent, EnviFlow enviFlow) {
		var result = parent.result;
		var impact = parent.impact;
		var impactFactor = result.getImpactFactorOf(impact, enviFlow);
		var inventoryVal = result.getDirectFlowOf(enviFlow, parent.techFlow);
		double impactVal = impactFactor * inventoryVal;
		return new TreeItem(
				result, parent.impact, parent, null, enviFlow, impactVal, inventoryVal);
	}

	static List<TreeItem> rootsOf(LcaResult result, List<ImpactDescriptor> impacts) {
		return impacts.stream()
				.map(impact -> rootOf(result, impact))
				.toList();
	}

	List<TreeItem> techNodesOf(List<TechFlow> techFlows, double cutoff) {
		return isRoot()
				? collect(techFlows, cutoff, TreeItem::techNodeOf)
				: Collections.emptyList();
	}

	List<TreeItem> techLeafsOf(List<TechFlow> techFlows, double cutoff) {
		return isEnviItem()
				? collect(techFlows, cutoff, TreeItem::techLeafOf)
				: Collections.emptyList();
	}

	List<TreeItem> enviNodesOf(List<EnviFlow> enviFlows, double cutoff) {
		return isRoot()
				? collect(enviFlows, cutoff, TreeItem::enviNodeOf)
				: Collections.emptyList();
	}

	List<TreeItem> enviLeafsOf(List<EnviFlow> enviFlows, double cutoff) {
		return isTechItem()
				? collect(enviFlows, cutoff, TreeItem::enviLeafOf)
				: Collections.emptyList();
	}

	private <E> List<TreeItem> collect(
			List<E> elements, double cutoff, BiFunction<TreeItem, E, TreeItem> fn) {
		double absMax = cutoff != 0
				? Math.abs(result.getTotalImpactValueOf(impact) * cutoff)
				: 0;
		return elements.stream()
				.map(e -> fn.apply(this, e))
				.filter(item -> item.impactResult != 0
						&& (cutoff == 0 || Math.abs(item.impactResult) >= absMax))
				.sorted((i1, i2) -> Double.compare(i2.impactResult, i1.impactResult))
				.toList();
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

	double contributionShare() {
		double total = result.getTotalImpactValueOf(impact);
		return Contribution.shareOf(impactResult, total);
	}

}
