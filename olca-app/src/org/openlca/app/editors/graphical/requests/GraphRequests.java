package org.openlca.app.editors.graphical.requests;

import org.eclipse.gef.RequestConstants;

public class GraphRequests implements RequestConstants {

	/// Add an input to a process.
	public static final String REQ_ADD_INPUT = "add_input_exchange";

	/// Add an output to a process.
	public static final String REQ_ADD_OUTPUT = "add_output_exchange";

	/// Collapse one or another side of a node.
	public static final String REQ_COLLAPSE = "collapse";

	/// Edit an element in an external widget (like a dialog).
	public static final String REQ_EDIT = "edit";

	/// Edit the general graph configuration.
	public static final String REQ_EDIT_CONFIG = "edit_config";

	/// Expand one or another side of a node.
	public static final String REQ_EXPAND = "expand";

	/// Expand or collapse one or another side of a node.
	public static final String REQ_EXPAND_OR_COLLAPSE = "expand_or_collapse";

	/// Apply the layout.
	public static final String REQ_LAYOUT = "layout";

	/// Maximize a node.
	public static final String REQ_MAXIMIZE = "max_all";

	/// Minimize a node
	public static final String REQ_MINIMIZE = "min_all";

	/// Set or remove analysis groups from processes.
	public static final String REQ_SET_PROCESS_GROUP = "set_process_group";

	/// Set an exchange as quantitative reference of a process.
	public static final String REQ_SET_REFERENCE = "set_reference";

	/// Remove the process chain of a selected link, exchange, or process.
	public static final String REQ_REMOVE_CHAIN = "remove_chain";

}
