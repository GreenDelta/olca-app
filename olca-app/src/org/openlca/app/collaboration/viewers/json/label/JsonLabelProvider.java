package org.openlca.app.collaboration.viewers.json.label;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jgit.diff.DiffEntry.Side;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;

public class JsonLabelProvider extends StyledCellLabelProvider {

	private final Side side;
	private final IJsonNodeLabelProvider nodeLabelProvider;
	private final LabelStyle style = new LabelStyle();

	public JsonLabelProvider(IJsonNodeLabelProvider nodeLabelProvider, Side side) {
		this.nodeLabelProvider = nodeLabelProvider;
		this.side = side;
	}

	@Override
	public void update(ViewerCell cell) {
		var element = cell.getElement();
		if (!(element instanceof JsonNode))
			return;
		var node = (JsonNode) element;
		var styledString = getStyledText(node);
		cell.setText(styledString.toString());
		cell.setStyleRanges(styledString.getStyleRanges());
		cell.setImage(nodeLabelProvider.getImage(node, side));
		super.update(cell);
	}

	private StyledString getStyledText(JsonNode node) {
		var text = nodeLabelProvider.getText(node, side);
		var otherText = nodeLabelProvider.getText(node, side == Side.NEW ? Side.OLD : Side.NEW);
		text = adjustMultiline(node, text, otherText);
		var styled = new StyledString(text);
		style.applyTo(styled, node);
		return styled;
	}

	private String adjustMultiline(JsonNode node, String value, String otherValue) {
		if (value == null)
			value = "";
		var count1 = countLines(value);
		var count2 = countLines(otherValue);
		if (count2 > count1) {
			for (int i = 1; i <= (count2 - count1); i++) {
				value += "\r\n";
			}
		}
		return value;
	}

	private int countLines(String value) {
		if (value == null)
			return 1;
		var index = -1;
		var count = 1;
		while ((index = value.indexOf("\n", index + 1)) != -1) {
			count++;
		}
		return count;
	}

	@Override
	public void dispose() {
		style.dispose();
		super.dispose();
	}

}
