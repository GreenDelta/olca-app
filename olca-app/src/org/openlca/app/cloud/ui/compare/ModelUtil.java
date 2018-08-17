package org.openlca.app.cloud.ui.compare;

import static org.openlca.app.cloud.ui.compare.json.JsonUtil.getString;

import java.util.HashMap;
import java.util.Map;

import org.openlca.app.cloud.ui.compare.json.IDependencyResolver;
import org.openlca.app.cloud.ui.compare.json.JsonNode;
import org.openlca.app.cloud.ui.compare.json.JsonUtil.ElementFinder;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.DQIndicator;
import org.openlca.core.model.DQScore;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.NwFactor;
import org.openlca.core.model.NwSet;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.SocialAspect;
import org.openlca.core.model.SocialIndicator;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.jsonld.input.Uncertainties;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ModelUtil {

	private static ElementFinder elementFinder = new ModelElementFinder();
	private static IDependencyResolver dependencyResolver = new ModelDependencyResolver();

	public static ElementFinder getElementFinder() {
		return elementFinder;
	}

	public static IDependencyResolver getDependencyResolver() {
		return dependencyResolver;
	}

	static boolean isReference(JsonElement parent, JsonElement element) {
		if (element == null)
			return false;
		if (!element.isJsonObject())
			return false;
		JsonObject object = element.getAsJsonObject();
		if (isType(object, Parameter.class))
			if (!isGlobalParameter(object))
				return false;
		if (isType(object, Exchange.class))
			if (isType(parent, Process.class))
				return false;
		if (isType(object, Unit.class))
			if (isType(parent, UnitGroup.class))
				return false;
		if (isType(object, ImpactCategory.class))
			return false;
		if (isType(object, NwSet.class))
			if (isType(parent, ImpactMethod.class))
				return false;
		if (object.get("@id") == null)
			return false;
		return true;
	}

	static String getObjectLabel(JsonElement parent, JsonObject o) {
		if (isType(o, ProjectVariant.class))
			return getProjectVariantLabel(o);
		if (o.has("name"))
			return o.get("name").getAsString();
		if (o.has("label"))
			return o.get("label").getAsString();
		if (isType(o, DQIndicator.class) || isType(o, DQScore.class))
			return o.get("position").getAsString();
		if (isType(parent, Flow.class) && isType(o, FlowPropertyFactor.class))
			return getString(o, "flowProperty.name");
		if (isType(parent, Process.class) && isType(o, Exchange.class))
			return getString(o, "flow.name");
		if (isType(o, SocialAspect.class))
			return getString(o, "socialIndicator.name");
		if (isType(o, ImpactFactor.class))
			return getString(o, "flow.name");
		if (isType(parent, ProductSystem.class) && isType(o, Exchange.class))
			return getString(o, "flow.name");
		if (isType(o, NwFactor.class))
			return getString(o, "impactCategory.name");
		if (isType(o, AllocationFactor.class))
			return getAllocationFactorLabel(o);
		if (isType(o, ProcessLink.class))
			return getProcessLinkLabel(o);
		if (isType(o, Uncertainty.class))
			return getUncertaintyLabel(o);
		return null;
	}

	private static String getProjectVariantLabel(JsonObject o) {
		String name = getString(o, "name");
		String productSystem = getString(o, "productSystem.name");
		return name + " - " + productSystem;
	}

	private static String getAllocationFactorLabel(JsonObject o) {
		String product = getString(o, "product.name");
		if (!o.has("exchange"))
			return product;
		String flow = getString(o, "exchange.flow.name");
		return product + " - " + flow;
	}

	private static String getProcessLinkLabel(JsonObject o) {
		String provider = getString(o, "provider.name");
		String output = getString(o, "providerOutput.flow.name");
		String recipient = getString(o, "recipient.name");
		String input = getString(o, "recipientInput.flow.name");
		return provider + "/" + output + " -> " + recipient + "/" + input;
	}

	private static String getUncertaintyLabel(JsonObject o) {
		Uncertainty u = Uncertainties.read(o);
		return Uncertainty.string(u);
	}

	private static boolean isGlobalParameter(JsonObject parameter) {
		JsonElement scope = parameter.get("parameterScope");
		if (scope == null)
			return false;
		return "GLOBAL_SCOPE".equals(scope.getAsString());
	}

	static boolean displayElement(String property) {
		return displayElement(null, property);
	}

	static boolean displayElement(JsonElement parent, String property) {
		if (property.startsWith("@"))
			return false;
		if (property.equals("internalId"))
			return false;
		if (property.equals("lastChange"))
			return false;
		if (property.equals("version"))
			return false;
		if (property.equals("parameterScope"))
			return false;
		if (parent == null || !parent.isJsonObject())
			return true;
		if (isType(parent, ProcessLink.class))
			return false;
		if (property.equals("name")) {
			if (isType(parent, Unit.class))
				return false;
			if (isType(parent, ProjectVariant.class))
				return false;
			if (isType(parent, Parameter.class))
				return false;
			if (isType(parent, ParameterRedef.class))
				return false;
			if (isType(parent, ImpactCategory.class))
				return false;
			if (isType(parent, NwSet.class))
				return false;
			if (isType(parent, DQIndicator.class))
				return false;
		}
		if (property.equals("label"))
			if (isType(parent, DQScore.class))
				return false;
		if (property.equals("productSystem"))
			if (isType(parent, ProjectVariant.class))
				return false;
		if (property.equals("flowProperty"))
			if (isType(parent, FlowPropertyFactor.class))
				return false;
		if (property.equals("input"))
			if (isType(parent, Exchange.class))
				return false;
		if (property.equals("flow")) {
			if (isType(parent, Exchange.class))
				return false;
			if (isType(parent, ImpactFactor.class))
				return false;
		}
		if (property.equals("impactCategory"))
			if (isType(parent, NwFactor.class))
				return false;
		if (property.equals("socialIndicator"))
			if (isType(parent, SocialAspect.class))
				return false;
		if (property.equals("product"))
			if (isType(parent, AllocationFactor.class))
				return false;
		if (property.equals("exchange"))
			if (isType(parent, AllocationFactor.class))
				return false;
		if (property.equals("lastModificationDate"))
			if (isType(parent, Project.class))
				return false;
		if (isType(parent, Uncertainty.class))
			if (!property.contains("Formula"))
				return false;
		return true;
	}

	static boolean isReadOnly(JsonNode node, String property) {
		if (node.parent != null && node.parent.readOnly)
			return true;
		JsonElement element = node.getElement();
		if (isType(element, Uncertainty.class))
			return true;
		if (!element.isJsonArray())
			return false;
		// array elements
		JsonElement parent = node.parent.getElement();
		if (isType(parent, ProductSystem.class))
			if ("processes".equals(property))
				return true;
			else if ("processLinks".equals(property))
				return true;
			else if ("parameterRedefs".equals(property))
				return true;
		if (isType(parent, DQSystem.class))
			if ("hasUncertainties".equals(property))
				return true;
		return false;
	}

	public static String getType(JsonElement element) {
		if (element == null)
			return null;
		if (!element.isJsonObject())
			return null;
		JsonElement type = element.getAsJsonObject().get("@type");
		if (type == null)
			return null;
		return type.getAsString();
	}

	static boolean isType(JsonElement element, Class<?> clazz) {
		String type = getType(element);
		return clazz.getSimpleName().equals(type);
	}

	private static class ModelElementFinder extends ElementFinder {

		@Override
		public String[] getComparisonFields(String property) {
			if (property.equals("units"))
				return new String[] { "name" };
			if (property.equals("flowProperties"))
				return new String[] { "flowProperty.@id" };
			if (property.equals("inputs") || property.equals("outputs") || property.equals("exchanges"))
				return new String[] { "internalId" };
			if (property.equals("socialAspects"))
				return new String[] { "socialIndicator.@id" };
			if (property.equals("synonyms"))
				return new String[0];
			if (property.equals("allocationFactors"))
				return new String[] { "product.@id", "exchange.internalId", "allocationType" };
			if (property.equals("impactFactors"))
				return new String[] { "flow.@id" };
			if (property.equals("factors"))
				return new String[] { "impactCategory.@id" };
			if (property.equals("variants"))
				return new String[] { "name", "productSystem.@id" };
			if (property.equals("parameterRedefs"))
				return new String[] { "name", "context.@id" };
			if (property.equals("indicators") || property.equals("scores"))
				return new String[] { "position" };
			return new String[] { "@id" };
		}

		@Override
		protected boolean skipOnEqualsCheck(String parentProperty,
				JsonElement parent, String property) {
			if (parentProperty == null)
				if ("version".equals(property))
					return true;
				else if ("lastChange".equals(property))
					return true;
			return false;
		}
	}

	private static class ModelDependencyResolver implements IDependencyResolver {

		private static final Map<String, Map<String, String>> dependencies = new HashMap<>();

		static {
			put(Exchange.class, "flowProperty", "unit");
			put(ImpactFactor.class, "flowProperty", "unit");
			put(SocialIndicator.class, "activityQuantity", "activityUnit");
			put(ProductSystem.class, "referenceProcess", "referenceExchange");
			put(ProductSystem.class, "referenceExchange", "targetFlowProperty");
			put(ProductSystem.class, "targetFlowProperty", "targetUnit");
			put(ProductSystem.class, "processes", "processLinks");
			put(ProductSystem.class, "processes", "parameterRedefs");
			put(ProductSystem.class, "processLinks", "parameterRedefs");
		}

		private static void put(Class<?> clazz, String from, String to) {
			Map<String, String> map = dependencies.get(clazz.getSimpleName());
			if (map == null)
				dependencies.put(clazz.getSimpleName(), map = new HashMap<>());
			map.put(from, to);
			map.put(to, from);
		}

		@Override
		public String resolve(JsonElement parent, String property) {
			String type = getType(parent);
			if (type == null)
				return null;
			Map<String, String> map = dependencies.get(type);
			if (map == null)
				return null;
			return map.get(property);
		}

	}

}
