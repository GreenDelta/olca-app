package org.openlca.app.collaboration.viewers.json.label;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;

class ColorStyler extends Styler {

	private Font font;
	private Color background;
	private Color foreground;
	private boolean strikeout;
	private boolean italic;

	ColorStyler foreground(Color foreground) {
		this.foreground = foreground;
		return this;
	}

	ColorStyler background(Color background) {
		this.background = background;
		return this;
	}

	ColorStyler strikeout() {
		this.strikeout = true;
		return this;
	}

	ColorStyler italic() {
		this.italic = true;
		return this;
	}

	@Override
	public void applyStyles(TextStyle textStyle) {
		if (background != null) {
			textStyle.background = background;
		}
		if (foreground != null) {
			textStyle.foreground = foreground;
		}
		textStyle.strikeout = strikeout;
		if (italic) {
			textStyle.font = getFont();
		}
	}

	private Font getFont() {
		if (font != null)
			return font;
		var desc = FontDescriptor.createFrom(
				Display.getCurrent().getSystemFont()).setStyle(SWT.ITALIC);
		font = desc.createFont(Display.getCurrent());
		return font;
	}

	void dispose() {
		if (font == null)
			return;
		font.dispose();
		font = null;
	}

}
