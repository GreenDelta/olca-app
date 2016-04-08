package org.openlca.app.cloud.ui.compare.json.viewer.label;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.openlca.app.cloud.ui.compare.json.JsonNode;
import org.openlca.app.cloud.ui.compare.json.JsonUtil;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Direction;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Side;

public class JsonTreeLabelProvider extends StyledCellLabelProvider {

	private final PropertyStyle propertyStyle = new PropertyStyle();
	private final DiffStyle diffStyle = new DiffStyle();
	private final ReadOnlyStyle readOnlyStyle = new ReadOnlyStyle();
	private Side side;
	private Direction direction;
	private IJsonNodeLabelProvider nodeLabelProvider;

	public JsonTreeLabelProvider(IJsonNodeLabelProvider nodeLabelProvider,
			Side side, Direction direction) {
		this.nodeLabelProvider = nodeLabelProvider;
		this.side = side;
		this.direction = direction;
	}

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		if (!(element instanceof JsonNode))
			return;
		JsonNode node = (JsonNode) element;
		StyledString styledString = getStyledText(node);
		cell.setText(styledString.toString());
		cell.setStyleRanges(styledString.getStyleRanges());
		cell.setImage(nodeLabelProvider.getImage(node, side));
		super.update(cell);
	}

	private StyledString getStyledText(JsonNode node) {
		String text = nodeLabelProvider.getText(node, side);
		String otherText = nodeLabelProvider.getText(node, side.getOther());
		text = adjustMultiline(node, text, otherText);
		StyledString styled = new StyledString(text);
		propertyStyle.applyTo(styled);
		if (node.readOnly)
			readOnlyStyle.applyTo(styled);
		if (node.hasEqualValues())
			return styled;
		if (direction == null)
			return styled;
		boolean highlightChanges = doHighlightChanges(node, otherText);
		diffStyle.applyTo(styled, otherText, side, direction,
				highlightChanges);
		return styled;
	}

	private boolean doHighlightChanges(JsonNode node, String otherText) {
		if (otherText == null)
			return false;
		if (!node.getElement().isJsonPrimitive())
			return false;
		if (!JsonUtil.toJsonPrimitive(node.getElement()).isString())
			return false;
		return true;
	}

	private String adjustMultiline(JsonNode node, String value,
			String otherValue) {
		if (value == null)
			value = "";
		int count1 = countLines(value);
		int count2 = countLines(otherValue);
		if (count2 > count1)
			for (int i = 1; i <= (count2 - count1); i++)
				value += "\n";
		return value;
	}

	private int countLines(String value) {
		if (value == null)
			return 0;
		int index = -1;
		int count = 0;
		while ((index = value.indexOf("\n", index + 1)) != -1)
			count++;
		return count;
	}

	@Override
	public void dispose() {
		readOnlyStyle.dispose();
		super.dispose();
	}

}
