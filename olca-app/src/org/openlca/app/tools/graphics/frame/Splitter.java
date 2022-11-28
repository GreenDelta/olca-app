package org.openlca.app.tools.graphics.frame;

import org.eclipse.swt.widgets.Composite;

public class Splitter extends Composite {

	public Splitter(Composite parent, int style) {
		super(parent, style);
		setLayout(new SplitterLayout());
	}

}
