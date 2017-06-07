package org.openlca.app.cloud.ui.compare.json.viewer.label;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.openlca.app.cloud.ui.compare.json.JsonNode;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Side;

public class JsonTreeLabelProvider extends StyledCellLabelProvider {

	private final Side side;
	private final IJsonNodeLabelProvider nodeLabelProvider;
	private final LabelStyle style = new LabelStyle();
	
	public JsonTreeLabelProvider(IJsonNodeLabelProvider nodeLabelProvider, Side side) {
		this.nodeLabelProvider = nodeLabelProvider;
		this.side = side;
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
		style.applyTo(styled, node);
		return styled;
	}

	private String adjustMultiline(JsonNode node, String value, String otherValue) {
		if (value == null)
			value = "";
		int count1 = countLines(value);
		int count2 = countLines(otherValue);
		if (count2 > count1)
			for (int i = 1; i <= (count2 - count1); i++)
				value += "\r\n";
		return value;
	}

	private int countLines(String value) {
		if (value == null)
			return 1;
		int index = -1;
		int count = 1;
		while ((index = value.indexOf("\n", index + 1)) != -1)
			count++;
		return count;
	}

	@Override
	public void dispose() {
		style.dispose();
		super.dispose();
	}

}
