package org.openlca.app.cloud.ui.compare.json.viewer.label;

import java.util.LinkedList;

import org.eclipse.jface.viewers.StyledString;
import org.openlca.app.cloud.ui.compare.json.viewer.label.DiffMatchPatch.Diff;
import org.openlca.app.cloud.ui.compare.json.viewer.label.DiffMatchPatch.Operation;
import org.openlca.app.cloud.ui.diff.ActionType;
import org.openlca.app.cloud.ui.diff.Site;
import org.openlca.app.util.Colors;

class DiffStyle {

	private ColorStyler deleteStyler = new ColorStyler().background(Colors.get(255, 230, 230)).strikeout();
	private ColorStyler insertStyler = new ColorStyler().background(Colors.get(230, 255, 230));
	private ColorStyler defaultStyler = new ColorStyler().background(Colors.get(240, 240, 240));

	void applyTo(StyledString styled, String otherText, Site site, ActionType action) {
		String text = styled.getString();
		if (text.isEmpty())
			return;
		styled.setStyle(0, text.length(), defaultStyler);
		LinkedList<Diff> diffs = getDiffs(text, otherText, site, action);
		boolean showDelete = doShowDelete(site, action);
		boolean showInsert = doShowInsert(site, action);
		int index = 0;
		for (Diff diff : diffs) {
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

	private LinkedList<Diff> getDiffs(String text, String otherText, Site site, ActionType action) {
		LinkedList<Diff> diffs = null;
		DiffMatchPatch dmp = new DiffMatchPatch();
		if (site == Site.LOCAL && action == ActionType.FETCH) {
			diffs = dmp.diff_main(text, otherText);
		} else if (site == Site.REMOTE && action == ActionType.COMMIT) {
			diffs = dmp.diff_main(text, otherText);
		} else {
			diffs = dmp.diff_main(otherText, text);
		}
		dmp.diff_cleanupSemantic(diffs);
		return diffs;
	}

	private boolean doShowDelete(Site site, ActionType action) {
		if (action == ActionType.COMMIT || action == ActionType.COMPARE_BEHIND)
			return site == Site.REMOTE;
		return site == Site.LOCAL;
	}

	private boolean doShowInsert(Site site, ActionType action) {
		if (action == ActionType.COMMIT || action == ActionType.COMPARE_BEHIND)
			return site == Site.LOCAL;
		return site == Site.REMOTE;
	}

}
