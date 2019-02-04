package org.openlca.app.editors.comments;

import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.DQIndicator;
import org.openlca.core.model.DQScore;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.NwFactor;
import org.openlca.core.model.NwSet;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.SocialAspect;
import org.openlca.core.model.Source;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;

// TODO: add null checks?
public class CommentPaths {

	public static String get(String path) {
		return path;
	}

	public static String get(Unit unit) {
		return "units[" + unit.name + "]";
	}

	public static String get(Source source) {
		return "documentation.sources[" + source.refId + "]";
	}

	public static String get(SocialAspect aspect) {
		return "socialAspects[" + aspect.indicator.refId + "]";
	}

	public static String get(Exchange exchange) {
		return "exchanges[" + exchange.internalId + "]";
	}

	public static String get(AllocationFactor factor, Exchange product) {
		return get(factor, product, null);
	}

	public static String get(AllocationFactor factor, Exchange product, Exchange exchange) {
		String type = factor.method.name();
		String id = product.flow.refId;
		if (exchange != null) {
			id += "-" + exchange.internalId;
		}
		return "allocationFactors[" + type.toLowerCase() + "-" + id + "]";
	}

	public static String get(Parameter parameter) {
		return "parameters[" + parameter.name + "]";
	}

	public static String get(FlowPropertyFactor factor) {
		return "flowProperties[" + factor.flowProperty.refId + "]";
	}

	public static String get(NwSet nwSet) {
		return "nwSets[" + nwSet.refId + "]";
	}

	public static String get(NwSet nwSet, NwFactor factor) {
		return get(nwSet) + ".factors[" + factor.impactCategory.refId + "]";
	}

	public static String get(ImpactCategory category) {
		return get(Descriptors.toDescriptor(category));
	}

	public static String get(ImpactCategoryDescriptor category) {
		return "impactCategories[" + category.refId + "]";
	}

	public static String get(ImpactCategory category, ImpactFactor factor) {
		return get(category) + ".impactFactors[" + factor.flow.refId + "]";
	}

	public static String get(DQIndicator indicator) {
		return "indicators[" + indicator.position + "]";
	}

	public static String get(DQScore score) {
		return "scores[" + score.position + "]";
	}

	public static String get(DQIndicator indicator, DQScore score) {
		return get(indicator) + "." + get(score);
	}

	public static String get(ParameterRedef redef, CategorizedDescriptor contextElement) {
		String context = contextElement != null ? contextElement.refId : "global";
		return "parameterRedefs[" + context + "-" + redef.name + "]";
	}

	public static String get(ProjectVariant variant) {
		return "variants[" + variant.productSystem.refId + "-" + variant.name + "]";
	}

	public static String get(ProjectVariant variant, ParameterRedef redef, CategorizedDescriptor contextElement) {
		return get(variant) + "." + get(redef, contextElement);
	}
}
