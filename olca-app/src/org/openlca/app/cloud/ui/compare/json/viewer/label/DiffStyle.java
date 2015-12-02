package org.openlca.app.cloud.ui.compare.json.viewer.label;

import java.util.LinkedList;

import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Direction;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Side;
import org.openlca.app.cloud.ui.compare.json.viewer.label.DiffMatchPatch.Diff;
import org.openlca.app.cloud.ui.compare.json.viewer.label.DiffMatchPatch.Operation;

class DiffStyle {

	private Styler deleteStyler = new DiffStyler(255, 230, 230, true);
	private Styler insertStyler = new DiffStyler(230, 255, 230, false);
	private Styler stringStyler = new DiffStyler(240, 240, 240, false);
	private Styler fieldStyler = new DiffStyler(255, 255, 128, false);

	void applyTo(StyledString styled, String otherText, Side side,
			Direction direction, boolean highlightChanges) {
		String text = styled.getString();
		int index = styled.getString().indexOf(":");
		Styler styler = highlightChanges ? stringStyler : fieldStyler;
		if (index == -1)
			styled.setStyle(0, text.length(), styler);
		else
			styled.setStyle(index + 2, text.length() - (index + 2), styler);
		if (highlightChanges)
			applySpecificDiffs(styled, otherText, side, direction);
	}

	private void applySpecificDiffs(StyledString styled, String otherText,
			Side side, Direction direction) {
		String text = styled.getString();
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
		if (side == Side.RIGHT && direction == Direction.LEFT_TO_RIGHT)
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
