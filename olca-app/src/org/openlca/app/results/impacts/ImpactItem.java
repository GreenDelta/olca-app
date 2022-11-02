package org.openlca.app.results.impacts;

import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.LcaResult;

record ImpactItem(
		int level,
		LcaResult result,
		ImpactDescriptor impact,
		TechFlow techFlow,
		EnviFlow enviFlow) {

	static ImpactItem rootOf(LcaResult result, ImpactDescriptor impact) {
		return new ImpactItem(0, result, impact, null, null);
	}

	static ImpactItem of(ImpactItem parent, TechFlow techFlow) {
		return new ImpactItem(
				parent.level + 1,
				parent.result,
				parent.impact,
				techFlow,
				parent.enviFlow);
	}

	static ImpactItem of(ImpactItem parent, EnviFlow enviFlow) {
		return new ImpactItem(
				parent.level + 1,
				parent.result,
				parent.impact,
				parent.techFlow,
				enviFlow);
	}

	/**
	 * The type of contribution shown by the item.
	 */
	ModelType type() {
		if (enviFlow != null)
			return ModelType.FLOW;
		if (techFlow != null)
			return ModelType.PROCESS;
		return ModelType.IMPACT_CATEGORY;
	}

	Double impactFactor() {
		return enviFlow != null
				? result.getImpactFactorOf(impact, enviFlow)
				: null;
	}

	String impactFactorUnit() {
		String iUnit = impact.referenceUnit;
		if (iUnit == null) {
			iUnit = "1";
		}
		String fUnit = enviFlow != null
				? Labels.refUnit(enviFlow)
				: "?";
		return iUnit + "/" + fUnit;
	}

	String impactFactorString() {
		if (type() != ModelType.FLOW)
			return null;
		var factor = impactFactor();
		if (factor == null)
			return null;
		return Numbers.format(factor) + " " + impactFactorUnit();
	}

	Double flowAmount() {
		if (enviFlow == null)
			return null;
		return techFlow == null
				? result.getTotalFlowValueOf(enviFlow)
				: result.getDirectFlowOf(enviFlow, techFlow);
	}

	String flowAmountString() {
		if (type() != ModelType.FLOW)
			return null;
		var amount = flowAmount();
		if (amount == null)
			return null;
		String unit = Labels.refUnit(enviFlow);
		return Numbers.format(amount) + " " + unit;
	}

	double amount() {
		return switch (type()) {
			case IMPACT_CATEGORY -> result.getTotalImpactValueOf(impact);
			case PROCESS -> result.getDirectImpactOf(impact, techFlow);
			case FLOW -> {
				var factor = impactFactor();
				var amount = flowAmount();
				yield factor == null || amount == null
						? 0
						: factor * amount;
			}
			default -> 0;
		};
	}

	String unit() {
		if (impact.referenceUnit == null)
			return null;
		return impact.referenceUnit;
	}

	String name() {
		return switch (type()) {
			case IMPACT_CATEGORY -> Labels.name(impact);
			case FLOW -> Labels.name(enviFlow);
			case PROCESS -> Labels.name(techFlow);
			default -> null;
		};
	}

	String category() {
		return switch (type()) {
			case IMPACT_CATEGORY -> Labels.category(impact);
			case FLOW -> Labels.category(enviFlow);
			case PROCESS -> Labels.category(techFlow);
			default -> null;
		};
	}

	double contribution() {
		double total = result.getTotalImpactValueOf(impact);
		return Contribution.shareOf(amount(), total);
	}
}
