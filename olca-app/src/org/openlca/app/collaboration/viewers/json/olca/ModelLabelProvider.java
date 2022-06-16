package org.openlca.app.collaboration.viewers.json.olca;

import org.eclipse.jgit.diff.DiffEntry.Side;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.collaboration.util.Json;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.collaboration.viewers.json.label.IJsonNodeLabelProvider;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;

public class ModelLabelProvider implements IJsonNodeLabelProvider {

	@Override
	public String getText(JsonNode node, Side side) {
		if (isFiller(node, side))
			return null;
		var propertyLabel = getPropertyText(node, side);
		if (showPropertyOnly(node, side))
			return propertyLabel;
		var valueLabel = getValueText(node, side);
		return propertyLabel + ": " + valueLabel;
	}

	@Override
	public String getPropertyText(JsonNode node, Side side) {
		if (isFiller(node, side))
			return null;
		if (node.parent != null && node.parent.element().isJsonArray())
			return node.property;
		return PropertyLabels.get(node);
	}

	@Override
	public String getValueText(JsonNode node, Side side) {
		if (isFiller(node, side))
			return null;
		return ValueLabels.get(node, side);
	}

	private boolean isFiller(JsonNode node, Side side) {
		var parent = node.parent.element(side);
		if (parent == null)
			return true;
		if (parent.isJsonNull())
			return true;
		if (parent.isJsonArray())
			if (parent.getAsJsonArray().size() == 0)
				return true;
		return false;
	}

	private boolean showPropertyOnly(JsonNode node, Side side) {
		var element = node.element(side);
		var otherElement = node.element(side == Side.NEW ? Side.OLD : Side.NEW);
		var isArrayElement = node.parent.element().isJsonArray();
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
		if (PropertyLabels.getImageType(node) != null)
			return false;
		if (node.property.equals("processDocumentation"))
			return true;
		return false;
	}

	@Override
	public Image getImage(JsonNode node, Side side) {
		var element = node.element(side);
		if (element == null)
			return null;
		var path = ModelUtil.pathOf(node);
		if (element.isJsonArray())
			return Images.get(PropertyLabels.getImageType(node));
		if (element.isJsonPrimitive())
			return null;
		if (path.equals("category")) {
			var parentType = Json.getModelType(node.parent.element(side));
			return getCategoryImage(parentType);
		}
		if (path.equals("impactFactors"))
			return Images.get(FlowType.ELEMENTARY_FLOW);
		if (path.equals("inputs") || path.equals("outputs")) {
			var flow = element.getAsJsonObject().get("flow");
			if (flow != null && flow.isJsonObject()) {
				var flowType = flow.getAsJsonObject().get("flowType");
				if (flowType != null) {
					return getFlowTypeImage(flowType.getAsString());
				}
			}
		}
		return Images.get(PropertyLabels.getImageType(node));

	}

	private Image getCategoryImage(ModelType type) {
		if (type == null)
			return null;
		return Images.getForCategory(type);
	}

	private Image getFlowTypeImage(String type) {
		for (var fType : FlowType.values())
			if (fType.name().equals(type))
				return Images.get(fType);
		return Images.get(ModelType.FLOW);
	}

}
