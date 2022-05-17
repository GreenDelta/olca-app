package org.openlca.app.editors.graph.model;

import org.openlca.app.editors.graph.GraphEditor;
import org.openlca.core.model.descriptors.RootDescriptor;

/**
 * An {@link IOPane} represents a list of exchanges to be displayed. Each
 * exchange is represented by a {@link ExchangeItem}.
 */
public class IOPane extends GraphComponent {

	private final boolean isInput;

	public IOPane(GraphEditor editor, boolean isInput) {
		super(editor);
		this.isInput = isInput;
	}

}
