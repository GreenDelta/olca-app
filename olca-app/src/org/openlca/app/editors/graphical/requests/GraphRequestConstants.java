package org.openlca.app.editors.graphical.requests;

import org.eclipse.gef.RequestConstants;

public class GraphRequestConstants implements RequestConstants {

	/**
	 * Indicates that the edit of an element should be performed through an edit
	 * page.
	 */
	public static String REQ_EDIT = "edit";

	/**
	 * Indicates the expansion of a node from one side or another.
	 */
	public static String REQ_EXPAND = "expand";

	/**
	 * Indicates the collapse of a node from one side or another.
	 */
	public static String REQ_COLLAPSE = "collapse";

	/**
	 * Indicates the expansion or the collapse of a node from one side or another.
	 */
	public static String REQ_EXPAND_OR_COLLAPSE = "expand_or_collapse";

	/**
	 * Indicates that the Graph diagram should be layout as a tree.
	 */
	public static String REQ_LAYOUT = "layout";

	/**
	 * Indicates that a component should be minimized.
	 */
	public static String REQ_MIN = "min_all";

	/**
	 * Indicates that a component should be maximized.
	 */
	public static String REQ_MAX = "max_all";

	/**
	 * Indicates the addition of an ExchangeItem in the input IOPane.
	 */
	public static String REQ_ADD_INPUT_EXCHANGE = "add_input_exchange";

	/**
	 * Indicates the addition of an ExchangeItem in the output IOPane.
	 */
	public static String REQ_ADD_OUTPUT_EXCHANGE = "add_output_exchange";

	/**
	 * Indicate that all the children of Graph should be reset in order to apply
	 * the new config.
	 */
	public static String REQ_EDIT_CONFIG = "edit_config";

}
