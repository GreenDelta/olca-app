package org.openlca.app.editors.sd.editor.graph.model;

/// A link can be a defined link between a stock and a flow, so that the
/// calculated flow values will update the stock in the iteration steps, or it
/// can be a link because of the usage of the source variable in the equation
/// that calculates the target variable.
public record SdVarLink(SdVarNode source, SdVarNode target, boolean isStockFlow) {

}
