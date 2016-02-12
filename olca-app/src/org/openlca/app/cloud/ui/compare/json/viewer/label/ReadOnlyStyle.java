package org.openlca.app.cloud.ui.compare.json.viewer.label;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.util.Colors;

class ReadOnlyStyle {

	private Font font;
	private Styler styler = new Styler() {

		@Override
		public void applyStyles(TextStyle textStyle) {
			textStyle.foreground = Colors.gray();
			textStyle.font = getFont();
		}
	};

	private Font getFont() {
		if (font != null)
			return font;
		FontDescriptor desc = FontDescriptor.createFrom(
				Display.getCurrent().getSystemFont()).setStyle(SWT.ITALIC);
		font = desc.createFont(Display.getCurrent());
		return font;
	}

	void applyTo(StyledString styled) {
		String text = styled.getString();
		styled.setStyle(0, text.length(), styler);
	}

	void dispose() {
		if (font != null)
			font.dispose();
	}

}
