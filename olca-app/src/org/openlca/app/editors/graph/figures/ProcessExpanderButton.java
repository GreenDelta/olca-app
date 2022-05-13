package org.openlca.app.editors.graph.figures;

import org.eclipse.draw2d.Button;
import org.eclipse.draw2d.ButtonBorder;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Label;

public class ProcessExpanderButton extends Button {

	public ProcessExpanderButton() {
		setBorder(new ButtonBorder(ButtonBorder.SCHEMES.TOOLBAR));
	}

	@Override
	public void setEnabled(boolean value) {
		super.setEnabled(value);
		var label = new Label("+");
		if (value)
			label.setForegroundColor(ColorConstants.black);
		else
			label.setForegroundColor(ColorConstants.gray);
		setContents(label);

	}

}
