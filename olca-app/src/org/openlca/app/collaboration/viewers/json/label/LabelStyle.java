package org.openlca.app.collaboration.viewers.json.label;

import org.eclipse.jface.viewers.StyledString;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.util.Colors;

class LabelStyle {

	private final ColorStyler propertyStyle = new ColorStyler().foreground(Colors.linkBlue());
	private final ColorStyler readOnlyStyle = new ColorStyler().background(Colors.gray()).italic();
	private final ColorStyler diffStyle = new ColorStyler().background(Colors.get(255, 255, 128));

	void applyTo(StyledString styled, JsonNode node) {
		var s = styled.getString();
		var colon = s.indexOf(':');
		apply(styled, propertyStyle, 0, colon + 1);
		if (node.readOnly) {
			apply(styled, readOnlyStyle);
		}
		if (node.hasEqualValues())
			return;
		if (colon != -1) {
			apply(styled, diffStyle, colon + 2, s.length() - colon - 2);
		} else {
			apply(styled, diffStyle);
		}
	}

	private void apply(StyledString styled, ColorStyler styler) {
		apply(styled, styler, 0, styled.getString().length());
	}

	private void apply(StyledString styled, ColorStyler styler, int start, int length) {
		var s = styled.getString();
		if (start >= s.length())
			return;
		if (length < 1) {
			length = s.length();
		} else if (length > s.length() - start) {
			length = s.length() - start;
		}
		styled.setStyle(start, length, styler);
	}

	void dispose() {
		readOnlyStyle.dispose();
		propertyStyle.dispose();
		diffStyle.dispose();
	}

}
