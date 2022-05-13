package org.openlca.app.editors.graph.figures;

import org.eclipse.draw2d.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

class NodeHeader extends Figure {

	private final String name;
	private final Image icon;

	NodeHeader(String name, Image icon) {
		this.name = name;
		this.icon = icon;

		GridLayout layout = new GridLayout(4, false);
		setLayoutManager(layout);

		var button1 = new ProcessExpanderButton();
		button1.setEnabled(true);
		add(button1, new GridData(SWT.LEAD, SWT.CENTER, false, false));

		add(new ImageFigure(icon), new GridData(SWT.LEAD, SWT.CENTER, false, false));

		var label = new Label(name);
		label.setForegroundColor(ColorConstants.black);
		add(label, new GridData(SWT.LEAD, SWT.CENTER, true, false));

		var button = new ProcessExpanderButton();
		button.setEnabled(true);
		add(button, new GridData(SWT.TRAIL, SWT.CENTER, false, false));
	}

}
