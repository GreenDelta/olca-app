package org.openlca.app.collaboration.viewers.json.label;

import java.util.LinkedList;

import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.diff.DiffEntry.Side;
import org.openlca.app.collaboration.viewers.json.label.DiffMatchPatch.Diff;
import org.openlca.app.collaboration.viewers.json.label.DiffMatchPatch.Operation;
import org.openlca.app.util.Colors;

public class DiffStyle {

	private ColorStyler deleteStyler = new ColorStyler().background(Colors.get(255, 230, 230)).strikeout();
	private ColorStyler insertStyler = new ColorStyler().background(Colors.get(230, 255, 230));
	private ColorStyler defaultStyler = new ColorStyler().background(Colors.get(240, 240, 240));

	public void applyTo(StyledString styled, String otherText, Side side) {
		var text = styled.getString();
		if (text.isEmpty())
			return;
		styled.setStyle(0, text.length(), defaultStyler);
		var diffs = getDiffs(text, otherText, side);
		boolean showDelete = side == Side.OLD;
		boolean showInsert = side == Side.NEW;
		int index = 0;
		for (var diff : diffs) {
			if (showDelete && diff.operation == Operation.DELETE) {
				styled.setStyle(index, diff.text.length(), deleteStyler);
				index += diff.text.length();
			} else if (showInsert && diff.operation == Operation.INSERT) {
				styled.setStyle(index, diff.text.length(), insertStyler);
				index += diff.text.length();
			} else if (diff.operation == Operation.EQUAL) {
				index += diff.text.length();
			}
		}
	}

	private LinkedList<Diff> getDiffs(String text, String otherText, Side side) {
		LinkedList<Diff> diffs = null;
		var dmp = new DiffMatchPatch();
		if (side == Side.OLD) {
			diffs = dmp.diff_main(text, otherText);
		} else {
			diffs = dmp.diff_main(otherText, text);
		}
		dmp.diff_cleanupSemantic(diffs);
		return diffs;
	}

}
