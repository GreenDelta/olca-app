package org.openlca.app.editors.graphical.action;

import org.openlca.app.editors.graphical.GraphEditor;

public interface GraphAction {

	/**
	 * Returns true if this action accepts the current state of the editor
	 * in order to be active. The given editor state may be used to initialize
	 * this action.
	 */
	boolean accepts(GraphEditor editor);

}
