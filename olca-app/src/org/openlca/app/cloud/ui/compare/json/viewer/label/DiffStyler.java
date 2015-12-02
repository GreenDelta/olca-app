package org.openlca.app.cloud.ui.compare.json.viewer.label;

import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.TextStyle;
import org.openlca.app.util.Colors;

class DiffStyler extends Styler {

	private final Color color;
	private final boolean strikeout;

	DiffStyler(int r, int g, int b, boolean strikeout) {
		color = Colors.getColor(r, g, b);
		this.strikeout = strikeout;
	}

	@Override
	public void applyStyles(TextStyle textStyle) {
		textStyle.background = color;
		textStyle.strikeout = strikeout;
	}

}