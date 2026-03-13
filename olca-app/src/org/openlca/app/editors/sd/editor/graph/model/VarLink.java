package org.openlca.app.editors.sd.editor.graph.model;

import java.util.Objects;

/// `VarLink` visualizes a link of a variable in a model. There are different
/// types of links here:
///
/// 1. Links between stocks and flows. For input flows, the flow is the source
/// and the stock the target. For output flows, it is the other way around, the
/// stock is the source and the flow the target.
///
/// 2. Usages of variables in equations. Stocks, flows (rates), and auxiliaries
/// are all variables of a system dynamics model. They can be used in equations
/// of other variables. In such a link, the variable that is used is the source
/// and the variable with the equation where it is used is the target.
///
/// 3. Variable bindings of product systems. The variables of the model can be
/// bound to the reference amount or parameter values of a product system. In
/// such links, the model variable is the source and the product system the
/// target.
public record VarLink(
	VarNode source, SdNode target, LinkType type
) {
	public VarLink {
		Objects.requireNonNull(source);
		Objects.requireNonNull(target);
		Objects.requireNonNull(type);
	}
}
