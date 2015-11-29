package org.openlca.app.cloud.ui.compare;

import static org.openlca.app.util.Images.getCategoryIcon;
import static org.openlca.app.util.Images.getIcon;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.cloud.ui.compare.json.IJsonNodeLabelProvider;
import org.openlca.app.cloud.ui.compare.json.JsonNode;
import org.openlca.app.cloud.ui.compare.json.JsonUtil;
import org.openlca.core.model.Category;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.NwFactor;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.SocialAspect;

import com.google.gson.JsonElement;

public class ModelLabelProvider implements IJsonNodeLabelProvider {

	@Override
	public String getText(JsonNode node, boolean local) {
		return getText(node.property, node.getElement(local), getParent(node));
	}

	private String getText(String property, JsonElement element,
			JsonElement parent) {
		if (isFiller(element, parent))
			return null;
		String type = ModelUtil.getType(parent);
		String propertyLabel = PropertyLabels.get(type, property);
		if (showKeyOnly(element, parent))
			return property;
		String value = getValue(element, parent);
		String valueLabel = ValueLabels.get(property, element, parent, value);
		return propertyLabel + ": " + valueLabel;
	}

	private boolean isFiller(JsonElement element, JsonElement parent) {
		if (element == null)
			return true;
		if (element.isJsonNull())
			if (!parent.isJsonObject())
				return true;
		return false;
	}

	private boolean showKeyOnly(JsonElement element, JsonElement parent) {
		if (element.isJsonArray())
			if (element.getAsJsonArray().size() != 0)
				return true;
		if (!element.isJsonObject())
			return false;
		if (!parent.isJsonArray() && !ModelUtil.isReference(parent, element))
			return true;
		return false;
	}

	private boolean isNullValue(JsonElement element) {
		if (element == null)
			return true;
		if (!element.isJsonArray())
			return false;
		return element.getAsJsonArray().size() == 0;
	}

	private String getValue(JsonElement element, JsonElement parent) {
		if (isNullValue(element))
			return null;
		if (!element.isJsonObject())
			return element.getAsString();
		return ModelUtil.getObjectLabel(parent, element.getAsJsonObject());
	}

	@Override
	public Image getImage(JsonNode node, boolean local) {
		return getImage(node.property, node.getElement(local), getParent(node));
	}

	private Image getImage(String property, JsonElement element,
			JsonElement parent) {
		if (element == null)
			return null;
		if (element.isJsonArray())
			return getArrayImage(property);
		if (element.isJsonPrimitive())
			return null;
		String type = ModelUtil.getType(element);
		if (type == null)
			return null;
		if (type.equals(Category.class.getSimpleName())) {
			String parentType = ModelUtil.getType(parent);
			return getCategoryImage(parentType);
		}
		return getElementImage(element);
	}

	private Image getCategoryImage(String type) {
		for (ModelType mType : ModelType.values()) {
			if (mType == ModelType.UNKNOWN)
				continue;
			if (mType.getModelClass().getSimpleName().equals(type))
				return getCategoryIcon(mType);
		}
		return null;
	}

	private Image getElementImage(JsonElement element) {
		String type = ModelUtil.getType(element);
		for (ModelType mType : ModelType.values()) {
			if (mType == ModelType.UNKNOWN)
				continue;
			if (mType.getModelClass().getSimpleName().equals(type))
				return getIcon(mType);
		}
		if (type.equals(FlowPropertyFactor.class.getSimpleName()))
			return getIcon(ModelType.FLOW_PROPERTY);
		if (type.equals(ImpactFactor.class.getSimpleName()))
			return getIcon(ModelType.FLOW);
		if (type.equals(SocialAspect.class.getSimpleName()))
			return getIcon(ModelType.SOCIAL_INDICATOR);
		if (type.equals(ParameterRedef.class.getSimpleName()))
			return getIcon(ModelType.PARAMETER);
		if (type.equals(NwFactor.class.getSimpleName()))
			return getIcon(ModelType.IMPACT_CATEGORY);
		if (type.equals(Exchange.class.getSimpleName())) {
			String flowType = JsonUtil.getString(element, "flow.flowType");
			return getFlowTypeImage(flowType);
		}
		return null;
	}

	private Image getFlowTypeImage(String type) {
		for (FlowType fType : FlowType.values())
			if (fType.name().equals(type))
				return getIcon(fType);
		return getIcon(ModelType.FLOW);
	}

	private Image getArrayImage(String property) {
		if (property.equals("units"))
			return getIcon(ModelType.UNIT);
		if (property.equals("flowProperties"))
			return getIcon(ModelType.FLOW_PROPERTY);
		if (property.equals("flowPropertyFactors"))
			return getIcon(ModelType.FLOW_PROPERTY);
		if (property.equals("inputs") || property.equals("outputs"))
			return getIcon(ModelType.FLOW);
		if (property.equals("parameters"))
			return getIcon(ModelType.PARAMETER);
		if (property.equals("parameterRedefs"))
			return getIcon(ModelType.PARAMETER);
		if (property.equals("socialAspects"))
			return getIcon(ModelType.SOCIAL_INDICATOR);
		if (property.equals("processes"))
			return getIcon(ModelType.PROCESS);
		if (property.equals("impactFactors"))
			return getIcon(ModelType.FLOW);
		if (property.equals("nwSets"))
			return getIcon(ModelType.NW_SET);
		if (property.equals("factors"))
			return getIcon(ModelType.IMPACT_CATEGORY);
		return null;
	}

	private JsonElement getParent(JsonNode node) {
		JsonElement parent = node.parent.getElement();
		if (!parent.isJsonArray())
			return parent;
		return node.parent.parent.getElement();
	}

}
