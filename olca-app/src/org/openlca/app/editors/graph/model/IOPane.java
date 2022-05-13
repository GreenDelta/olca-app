package org.openlca.app.editors.graph.model;

/**
 * An {@link IOPane} represents a list of exchanges to be displayed. Each
 * exchange is represented by a {@link ExchangeItem}.
 */
public class IOPane extends GraphComponent {

	private final boolean isInput;

	public IOPane(boolean isInput) {
		this.isInput = isInput;
	}

}
