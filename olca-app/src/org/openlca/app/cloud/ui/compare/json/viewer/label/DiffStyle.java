package org.openlca.app.cloud.ui.compare.json.viewer.label;

import java.util.LinkedList;

import org.eclipse.jface.viewers.StyledString;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Direction;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Side;
import org.openlca.app.cloud.ui.compare.json.viewer.label.DiffMatchPatch.Diff;
import org.openlca.app.cloud.ui.compare.json.viewer.label.DiffMatchPatch.Operation;
import org.openlca.app.util.Colors;

class DiffStyle {

	private ColorStyler deleteStyler = new ColorStyler().background(Colors.get(255, 230, 230)).strikeout();
	private ColorStyler insertStyler = new ColorStyler().background(Colors.get(230, 255, 230));
	private ColorStyler defaultStyler = new ColorStyler().background(Colors.get(240, 240, 240));

	void applyTo(StyledString styled, String otherText, Side side, Direction direction) {
		String text = styled.getString();
		if (text.isEmpty())
			return;
		styled.setStyle(0, text.length(), defaultStyler);
		LinkedList<Diff> diffs = getDiffs(text, otherText, side, direction);
		boolean showDelete = doShowDelete(side, direction);
		boolean showInsert = doShowInsert(side, direction);
		int index = 0;
		for (Diff diff : diffs) {
			if (showDelete && diff.operation == Operation.DELETE) {
				styled.setStyle(index, diff.text.length(), deleteStyler);
				index += diff.text.length();
			} else if (showInsert && diff.operation == Operation.INSERT) {
				styled.setStyle(index, diff.text.length(), insertStyler);
				index += diff.text.length();
			} else if (diff.operation == Operation.EQUAL)
				index += diff.text.length();
		}
	}

	private LinkedList<Diff> getDiffs(String text, String otherText, Side side,
			Direction direction) {
		LinkedList<Diff> diffs = null;
		DiffMatchPatch dmp = new DiffMatchPatch();
		if (side == Side.LEFT && direction == Direction.RIGHT_TO_LEFT)
			diffs = dmp.diff_main(text, otherText);
		else if (side == Side.RIGHT && direction == Direction.LEFT_TO_RIGHT)
			diffs = dmp.diff_main(text, otherText);
		else
			diffs = dmp.diff_main(otherText, text);
		dmp.diff_cleanupSemantic(diffs);
		return diffs;
	}

	private boolean doShowDelete(Side side, Direction direction) {
		if (direction == Direction.LEFT_TO_RIGHT)
			return side == Side.RIGHT;
		return side == Side.LEFT;
	}

	private boolean doShowInsert(Side side, Direction direction) {
		if (direction == Direction.LEFT_TO_RIGHT)
			return side == Side.LEFT;
		return side == Side.RIGHT;
	}

}
