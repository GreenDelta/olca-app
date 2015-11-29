package org.openlca.app.cloud.ui.compare;

import org.openlca.app.cloud.ui.compare.json.JsonUtil;
import org.openlca.app.cloud.ui.compare.json.JsonUtil.ElementFinder;
import org.openlca.core.model.AllocationFactor;
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
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.SocialAspect;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class ModelUtil {

	private static ElementFinder elementFinder = new ModelElementFinder();
	
	static ElementFinder getElementFinder() {
		return elementFinder;
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

	static String getObjectLabel(JsonElement parent, JsonObject e) {
		if (e.has("name"))
			return e.get("name").getAsString();
		if (isType(parent, Flow.class) && isType(e, FlowPropertyFactor.class))
			return JsonUtil.getString(e, "flowProperty.name");
		if (isType(parent, Process.class) && isType(e, Exchange.class))
			return JsonUtil.getString(e, "flow.name");
		if (isType(parent, Process.class) && isType(e, SocialAspect.class))
			return JsonUtil.getString(e, "socialIndicator.name");
		if (isType(parent, ImpactCategory.class)
				&& isType(e, ImpactFactor.class))
			return JsonUtil.getString(e, "flow.name");
		if (isType(parent, ProductSystem.class) && isType(e, Exchange.class))
			return JsonUtil.getString(e, "flow.name");
		if (isType(parent, NwSet.class) && isType(e, NwFactor.class))
			return JsonUtil.getString(e, "impactCategory.name");
		if (isType(parent, Process.class) && isType(e, AllocationFactor.class)) {
			if (e.has("exchange"))
				return JsonUtil.getString(e, "product.name") + " - "
						+ JsonUtil.getString(e, "exchange.flow.name");
			else
				return JsonUtil.getString(e, "product.name");
		}
		if (isType(parent, ProductSystem.class) && isType(e, ProcessLink.class))
			return JsonUtil.getString(e, "provider.name") + "/"
					+ JsonUtil.getString(e, "providerOutput.flow.name")
					+ " -> " + JsonUtil.getString(e, "recipient.name") + "/"
					+ JsonUtil.getString(e, "recipientInput.flow.name");
		System.out.println(getType(parent) + " - " + getType(e));
		return null;
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
		}
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
		return true;
	}

	static String getType(JsonElement element) {
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
		public String[] getComparisonFields(String propery) {
			if (propery.equals("units"))
				return new String[] { "name" };
			if (propery.equals("flowProperties"))
				return new String[] { "flowProperty.@id" };
			if (propery.equals("inputs") || propery.equals("outputs"))
				return new String[] { "flow.@id", "input" };
			if (propery.equals("synonyms"))
				return new String[0];
			if (propery.equals("allocationFactors"))
				return new String[] { "product.@id", "exchange.flow.@id",
						"allocationType" };
			if (propery.equals("impactFactors"))
				return new String[] { "flow.@id" };
			if (propery.equals("factors"))
				return new String[] { "impactCategory.@id" };
			if (propery.equals("variants"))
				return new String[] { "name", "productSystem.@id" };
			if (propery.equals("parameterRedefs"))
				return new String[] { "name", "context.@id" };
			return new String[] { "@id" };
		}
	}

}
