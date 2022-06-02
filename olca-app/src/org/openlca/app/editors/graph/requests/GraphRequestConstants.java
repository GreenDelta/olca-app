package org.openlca.app.editors.graph.requests;

import org.eclipse.gef.RequestConstants;

public class GraphRequestConstants implements RequestConstants {

	/**
	 * Indicates that the edit of an element should be performed through an edit
	 * page.
	 */
	public static String REQ_EDIT = "edit";

	/**
	 * Indicates that the edit of an element should be performed through an edit
	 * page.
	 */
	public static String REQ_EXPANSION = "collapse";

	/**
	 * Indicates that the Graph diagram should be layout as a tree.
	 */
	public static String REQ_LAYOUT = "layout";

	/**
	 * Indicates the addition of an ExchangeItem in the input IOPane.
	 */
	public static String REQ_ADD_INPUT_EXCHANGE = "add_input_exchange";

	/**
	 * Indicates the addition of an ExchangeItem in the output IOPane.
	 */
	public static String REQ_ADD_OUTPUT_EXCHANGE = "add_output_exchange";

	/**
	 * Indicates the addition of a Node in the Graph.
	 */
	public static String REQ_ADD_PROCESS = "add_process";


}
