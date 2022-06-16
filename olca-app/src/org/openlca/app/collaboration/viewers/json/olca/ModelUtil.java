package org.openlca.app.collaboration.viewers.json.olca;

import static org.openlca.app.collaboration.util.Json.getString;

import java.util.Arrays;

import org.eclipse.jgit.diff.DiffEntry.Side;
import org.openlca.app.collaboration.util.Json;
import org.openlca.app.collaboration.util.Json.ElementFinder;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Uncertainty;
import org.openlca.jsonld.input.Uncertainties;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class ModelUtil {

	static final ElementFinder ELEMENT_FINDER = new ModelElementFinder();

	static String getObjectLabel(JsonNode node, Side side) {
		var element = node.element(side).getAsJsonObject();
		var path = pathOf(node);
		if (path.equals("variants"))
			return getProjectVariantLabel(element);
		if (element.has("name"))
			return getString(element, "name");
		if (element.has("label"))
			return getString(element, "label");
		if (path.equals("indicators") || path.equals("indicators.scores"))
			return getString(element, "position");
		if (path.equals("flowProperties"))
			return getString(element, "flowProperty.name");
		if (path.equals("inputs") || path.equals("outputs") || path.equals("impactFactors")
				|| path.equals("inputResults") || path.equals("outputResults")
				|| path.equals("refExchange") || path.equals("product"))
			return getString(element, "flow.name");
		if (path.equals("impactResults"))
			return getString(element, "indicator.name");
		if (path.equals("socialAspects"))
			return getString(element, "socialIndicator.name");
		if (path.equals("nwSets.factors"))
			return getString(element, "impactCategory.name");
		if (path.equals("allocationFactors"))
			return getAllocationFactorLabel(element);
		if (path.contains(".uncertainty") || path.startsWith("uncertainty"))
			return getUncertaintyLabel(element);
		if (path.equals("geometry"))
			return getString(element, "type");
		return null;
	}

	private static String getProjectVariantLabel(JsonObject o) {
		var name = getString(o, "name");
		var productSystem = getString(o, "productSystem.name");
		return name + " - " + productSystem;
	}

	private static String getAllocationFactorLabel(JsonObject o) {
		var product = getString(o, "product.name");
		if (!o.has("exchange"))
			return product;
		var flow = getString(o, "exchange.flow.name");
		return product + " - " + flow;
	}

	private static String getUncertaintyLabel(JsonObject o) {
		var u = Uncertainties.read(o);
		return Uncertainty.string(u);
	}

	static boolean displayElement(JsonNode node, String property) {
		var parentPath = pathOf(node);
		var path = !parentPath.isEmpty()
				? parentPath + "." + property
				: property;
		var hiddenProps = Arrays.asList("@id", "@type", "lastChange", "version", "internalId", "lastInternalId",
				"precedingDataSet", "position", "parameterScope", "context");
		var hiddenPaths = Arrays.asList("flowProperties.flowProperty", "impactFactors.flow", "product.flow",
				"nwSets.name", "nwSet.name", "nwSets.factors.impactCategory", "socialAspects.socialIndicator",
				"variants.productSystem", "inputs.isInput", "outputs.isInput", "inputs.flow", "outputs.flow",
				"inputResults.isInput", "outputResults.isInput", "inputResults.flow.flowType",
				"outputResults.flow.flowType", "inputResults.flow.refUnit", "outputResults.flow.refUnit",
				"allocationFactors.product", "allocationFactors.exchange", "variants.name", "indicators.name");
		var hiddenRefs = Arrays.asList("processLinks", "category", "refProcess", "refExchange",
				"activityQuantity", "targetFlowProperty", "impactCategories");
		if (hiddenProps.contains(property) || hiddenPaths.contains(path) || hiddenRefs.contains(parentPath))
			return false;
		if (parentPath.contains(".uncertainty") || parentPath.startsWith("uncertainty")
				|| parentPath.equals("geometry"))
			return false;
		if (property.equals("refUnit") && parentPath.endsWith(".flowProperty") || parentPath.equals("defaultFlowProperty"))
			return false;
		if ((property.equals("name") || property.equals("category")) && PropertyLabels.getImageType(node) != null)
			return false;
		return true;
	}

	static boolean isReadOnly(JsonNode node) {
		if (node.parent != null && node.parent.readOnly)
			return true;
		var element = node.element();
		var path = pathOf(node);
		if (!element.isJsonArray())
			return false;
		// array elements
		var type = typeOf(node);
		if (type == ModelType.PRODUCT_SYSTEM) {
			if (path.equals("processes"))
				return true;
			if (path.equals("processLinks"))
				return true;
			if (path.equals("parameterSets"))
				return true;
		}
		if (type == ModelType.DQ_SYSTEM && path.equals("hasUncertainties"))
			return true;
		return false;
	}

	static ModelType typeOf(JsonNode node) {
		while (node.parent != null) {
			node = node.parent;
		}
		return Json.getModelType(node.element());
	}

	static String pathOf(JsonNode node) {
		if (node == null)
			return "";
		var parentPath = pathOf(node.parent);
		var property = "";
		if (node.property != null && (node.parent == null || !node.parent.element().isJsonArray())) {
			property = node.property;
		}
		if (parentPath.isEmpty())
			return property;
		if (property.isEmpty())
			return parentPath;
		return parentPath + "." + property;
	}

	static String valueOf(JsonNode node, Side side) {
		var element = node.element(side);
		if (element == null)
			return "";
		if (element.isJsonArray() && element.getAsJsonArray().size() == 0)
			return "";
		if (element.isJsonNull())
			return "";
		if (!element.isJsonObject())
			return element.getAsString();
		return ModelUtil.getObjectLabel(node, side);
	}

	private static class ModelElementFinder extends ElementFinder {

		@Override
		public String[] getComparisonFields(String property) {
			if (property.equals("units"))
				return new String[] { "name" };
			if (property.equals("flowProperties"))
				return new String[] { "flowProperty.@id" };
			if (property.equals("inputs") || property.equals("outputs"))
				return new String[] { "internalId" };
			if (property.equals("socialAspects"))
				return new String[] { "socialIndicator.@id" };
			if (property.equals("synonyms"))
				return new String[0];
			if (property.equals("allocationFactors"))
				return new String[] { "product.@id", "exchange.internalId", "allocationType" };
			if (property.equals("impactFactors") || property.equals("inputResults") || property.equals("outputResults"))
				return new String[] { "flow.@id" };
			if (property.equals("factors") || property.equals("impactResults"))
				return new String[] { "impactCategory.@id" };
			if (property.equals("variants"))
				return new String[] { "name", "productSystem.@id" };
			if (property.equals("modules"))
				return new String[] { "name", "result.@id" };
			if (property.equals("parameterRedefs"))
				return new String[] { "name", "context.@id" };
			if (property.equals("parameters"))
				return new String[] { "name" };
			if (property.equals("parameterSets"))
				return new String[] { "name" };
			if (property.equals("indicators") || property.equals("scores"))
				return new String[] { "position" };
			return new String[] { "@id" };
		}

		@Override
		protected boolean skipOnEqualsCheck(String parentProperty, JsonElement parent, String property) {
			if (parentProperty == null)
				if ("version".equals(property))
					return true;
				else if ("lastChange".equals(property))
					return true;
			return false;
		}
	}

}
