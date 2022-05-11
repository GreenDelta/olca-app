package org.openlca.app.editors.graph.model;

import org.eclipse.draw2d.geometry.Dimension;

public class IOPanel extends ConnectableModelElement {

	private boolean minimized = true;
	private final boolean isInput;


	public IOPanel(boolean isInput) {
		this.isInput = isInput;
		size = new Dimension(20, 20);
		System.out.println("Creating an IOPanel.");
	}

}
