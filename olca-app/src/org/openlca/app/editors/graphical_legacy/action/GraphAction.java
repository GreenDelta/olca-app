package org.openlca.app.editors.graphical_legacy.action;

import org.eclipse.jface.action.IAction;
import org.openlca.app.editors.graphical_legacy.GraphEditor;

public interface GraphAction extends IAction {

	/**
	 * Returns true if this action accepts the current state of the editor in order
	 * to be active. The given editor state may be used to initialize this action.
	 */
	boolean accepts(GraphEditor editor);

}
