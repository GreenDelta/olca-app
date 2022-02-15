package org.openlca.app.collaboration.viewers.json.olca;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.collaboration.util.Json;
import org.openlca.app.collaboration.viewers.json.Side;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.collaboration.viewers.json.label.IJsonNodeLabelProvider;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.Category;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.NwFactor;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.SocialAspect;
import org.openlca.core.model.Uncertainty;

import com.google.gson.JsonElement;

public class ModelLabelProvider implements IJsonNodeLabelProvider {

	@Override
	public String getText(JsonNode node, Side side) {
		var element = node.element(side);
		var otherElement = node.element(side.getOther());
		if (isFiller(element, node.parent.element(side)))
			return null;
		var propertyLabel = getPropertyText(node, side);
		var isArrayElement = node.parent.element().isJsonArray();
		var parent = getParent(node);
		if (showPropertyOnly(element, otherElement, parent, isArrayElement))
			return propertyLabel;
		var valueLabel = getValueText(node, side);
		return propertyLabel + ": " + valueLabel;
	}

	@Override
	public String getPropertyText(JsonNode node, Side side) {
		var element = node.element(side);
		if (isFiller(element, node.parent.element(side)))
			return null;
		var parent = getParent(node);
		var type = ModelUtil.getType(parent);
		return PropertyLabels.get(type, node.property);
	}

	@Override
	public String getValueText(JsonNode node, Side side) {
		var element = node.element(side);
		if (isFiller(element, node.parent.element(side)))
			return null;
		var parent = getParent(node);
		var value = getValue(element, parent);
		return ValueLabels.get(node.property, element, parent, value);
	}

	private JsonElement getParent(JsonNode node) {
		var parent = node.parent.element();
		if (!parent.isJsonArray())
			return parent;
		return node.parent.parent.element();
	}

	private boolean isFiller(JsonElement element, JsonElement parent) {
		if (parent == null)
			return true;
		if (parent.isJsonNull())
			return true;
		if (parent.isJsonArray())
			if (parent.getAsJsonArray().size() == 0)
				return true;
		return false;
	}

	private boolean showPropertyOnly(JsonElement element, JsonElement otherElement, JsonElement parent,
			boolean isArrayElement) {
		if (element == null || element.isJsonNull())
			if (otherElement == null || otherElement.isJsonArray())
				return true;
			else
				return false;
		if (element.isJsonArray())
			return true;
		if (!element.isJsonObject())
			return false;
		if (isArrayElement)
			return false;
		if (ModelUtil.isType(element, Uncertainty.class))
			return false;
		if (!ModelUtil.isReference(parent, element))
			return true;
		return false;
	}

	private String getValue(JsonElement element, JsonElement parent) {
		if (element == null)
			return "";
		if (element.isJsonArray() && element.getAsJsonArray().size() == 0)
			return "";
		if (element.isJsonNull())
			return "";
		if (!element.isJsonObject())
			return element.getAsString();
		return ModelUtil.getObjectLabel(parent, element.getAsJsonObject());
	}

	@Override
	public Image getImage(JsonNode node, Side side) {
		var parent = node.parent.element();
		if (parent.isJsonArray()) {
			parent = node.parent.parent.element();
		}
		return getImage(node.property, node.element(side), parent);
	}

	private Image getImage(String property, JsonElement element, JsonElement parent) {
		if (element == null)
			return null;
		if (element.isJsonArray())
			return getArrayImage(property);
		if (element.isJsonPrimitive())
			return null;
		var type = ModelUtil.getType(element);
		if (type == null)
			return null;
		if (type.equals(Category.class.getSimpleName())) {
			var parentType = ModelUtil.getType(parent);
			return getCategoryImage(parentType);
		}
		return getElementImage(element);
	}

	private Image getCategoryImage(String type) {
		for (ModelType mType : ModelType.values()) {
			if (mType == ModelType.UNKNOWN)
				continue;
			if (mType.getModelClass().getSimpleName().equals(type))
				return Images.getForCategory(mType);
		}
		return null;
	}

	private Image getElementImage(JsonElement element) {
		var type = ModelUtil.getType(element);
		for (ModelType mType : ModelType.values()) {
			if (mType == ModelType.UNKNOWN)
				continue;
			if (mType.getModelClass().getSimpleName().equals(type))
				return Images.get(mType);
		}
		if (type.equals(FlowPropertyFactor.class.getSimpleName()))
			return Images.get(ModelType.FLOW_PROPERTY);
		if (type.equals(ImpactFactor.class.getSimpleName()))
			return Images.get(ModelType.FLOW);
		if (type.equals(SocialAspect.class.getSimpleName()))
			return Images.get(ModelType.SOCIAL_INDICATOR);
		if (type.equals(ParameterRedef.class.getSimpleName()))
			return Images.get(ModelType.PARAMETER);
		if (type.equals(NwFactor.class.getSimpleName()))
			return Images.get(ModelType.IMPACT_CATEGORY);
		if (type.equals(Exchange.class.getSimpleName())) {
			var flowType = Json.getString(element, "flow.flowType");
			return getFlowTypeImage(flowType);
		}
		return null;
	}

	private Image getFlowTypeImage(String type) {
		for (var fType : FlowType.values())
			if (fType.name().equals(type))
				return Images.get(fType);
		return Images.get(ModelType.FLOW);
	}

	private Image getArrayImage(String property) {
		if (property.equals("units"))
			return Images.get(ModelType.UNIT);
		if (property.equals("flowProperties"))
			return Images.get(ModelType.FLOW_PROPERTY);
		if (property.equals("flowPropertyFactors"))
			return Images.get(ModelType.FLOW_PROPERTY);
		if (property.equals("inputs") || property.equals("outputs"))
			return Images.get(ModelType.FLOW);
		if (property.equals("parameters"))
			return Images.get(ModelType.PARAMETER);
		if (property.equals("parameterRedefs"))
			return Images.get(ModelType.PARAMETER);
		if (property.equals("parameterSets"))
			return Images.get(ModelType.PARAMETER);
		if (property.equals("socialAspects"))
			return Images.get(ModelType.SOCIAL_INDICATOR);
		if (property.equals("processes"))
			return Images.get(ModelType.PROCESS);
		if (property.equals("impactFactors"))
			return Images.get(ModelType.FLOW);
		if (property.equals("nwSets"))
			return Images.get(ModelType.NW_SET);
		if (property.equals("factors"))
			return Images.get(ModelType.IMPACT_CATEGORY);
		return null;
	}

}
