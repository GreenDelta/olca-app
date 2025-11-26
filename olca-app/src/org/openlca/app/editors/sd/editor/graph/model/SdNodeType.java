package org.openlca.app.editors.sd.editor.graph.model;

/**
 * Enum representing the different types of nodes in a system dynamics model.
 */
public enum SdNodeType {
	/**
	 * A stock represents an accumulation, a level variable that changes
	 * over time through inflows and outflows (rates).
	 */
	STOCK,

	/**
	 * A rate (or flow) represents the rate of change of a stock.
	 * It connects stocks together, representing material or information flow.
	 */
	RATE,

	/**
	 * An auxiliary represents a variable that is calculated from other
	 * variables. It can be a constant, a computed value, or a table function.
	 */
	AUXILIARY
}
