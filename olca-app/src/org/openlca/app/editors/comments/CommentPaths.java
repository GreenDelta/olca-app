package org.openlca.app.editors.comments;

import org.openlca.core.model.AllocationFactor;
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
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

public class CommentPaths {

	public static String get(Unit unit) {
		return unit != null
				? "units[" + unit.name + "]"
				: null;
	}

	public static String get(Source source) {
		return source != null
				? "documentation.sources[" + source.refId + "]"
				: null;
	}

	public static String get(SocialAspect aspect) {
		return aspect != null && aspect.indicator != null
				? "socialAspects[" + aspect.indicator.refId + "]"
				: null;
	}

	public static String get(Exchange exchange) {
		return exchange != null
				? "exchanges[" + exchange.internalId + "]"
				: null;
	}

	public static String get(AllocationFactor factor, Exchange product) {
		return get(factor, product, null);
	}

	public static String get(AllocationFactor factor, Exchange product, Exchange exchange) {
		if (factor == null
				|| factor.method == null
				|| product == null
				|| product.flow == null)
			return null;
		String type = factor.method.name();
		String id = product.flow.refId;
		if (exchange != null) {
			id += "-" + exchange.internalId;
		}
		return "allocationFactors[" + type.toLowerCase() + "-" + id + "]";
	}

	public static String get(Parameter parameter) {
		return parameter != null
				? "parameters[" + parameter.name + "]"
				: null;
	}

	public static String get(FlowPropertyFactor factor) {
		return factor != null && factor.flowProperty != null
				? "flowProperties[" + factor.flowProperty.refId + "]"
				: null;
	}

	public static String get(NwSet nwSet) {
		return nwSet != null
				? "nwSets[" + nwSet.refId + "]"
				: null;
	}

	public static String get(NwSet nwSet, NwFactor factor) {
		return nwSet != null && factor != null && factor.impactCategory != null
				? get(nwSet) + ".factors[" + factor.impactCategory.refId + "]"
				: null;
	}

	public static String get(ImpactCategory category) {
		return category != null
				? get(Descriptor.of(category))
				: null;
	}

	public static String get(ImpactDescriptor category) {
		return category != null
				? "impactCategories[" + category.refId + "]"
				: null;
	}

	public static String get(ImpactCategory category, ImpactFactor factor) {
		return category != null && factor != null && factor.flow != null
				? get(category) + ".impactFactors[" + factor.flow.refId + "]"
				: null;
	}

	public static String get(ParameterRedef redef, RootDescriptor context) {
		if (redef == null)
			return null;
		var ctx = context != null ? context.refId : "global";
		return "parameterRedefs[" + ctx + "-" + redef.name + "]";
	}

	public static String get(ProjectVariant variant) {
		return variant != null && variant.productSystem != null
				? "variants[" + variant.productSystem.refId + "-" + variant.name + "]"
				: null;
	}

	public static String get(
			ProjectVariant variant, ParameterRedef redef, RootDescriptor context) {
		return variant != null && redef != null && context != null
				? get(variant) + "." + get(redef, context)
				: null;
	}
}
