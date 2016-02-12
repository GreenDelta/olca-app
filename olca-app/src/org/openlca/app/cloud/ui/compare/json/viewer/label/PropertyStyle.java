package org.openlca.app.cloud.ui.compare.json.viewer.label;

import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.TextStyle;
import org.openlca.app.util.Colors;

class PropertyStyle {

	private Styler styler = new Styler() {
		@Override
		public void applyStyles(TextStyle textStyle) {
			textStyle.foreground = Colors.linkBlue();
		}
	};

	void applyTo(StyledString styled) {
		String text = styled.getString();
		int index = text.indexOf(":");
		if (index == -1)
			styled.setStyle(0, text.length(), styler);
		else
			styled.setStyle(0, index + 1, styler);
	}

}