package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.commands.Command;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.editors.sd.editor.graph.model.VarNode;
import org.openlca.app.editors.sd.editor.graph.view.AuxFigure;
import org.openlca.app.editors.sd.editor.graph.view.FlowFigure;
import org.openlca.app.editors.sd.editor.graph.view.StockFigure;
import org.openlca.sd.model.Auxil;
import org.openlca.sd.model.Rate;


public final class VarPart extends NodePart<VarNode> {

	VarPart(VarNode model, Theme theme) {
		super(model, theme);
	}

	@Override
	protected IFigure createFigure() {
		var model = getModel();
		if (model == null) return new StockFigure(theme);
		return switch (getModel().variable()) {
			case Auxil ignore -> new AuxFigure(theme);
			case Rate ignore -> new FlowFigure(theme);
			case null, default -> new StockFigure(theme);
		};
	}

	@Override
	protected Command getDeleteCommand() {
		var graph = getGraph();
		var node = getModel();
		return graph != null && node != null
			? new VarDeleteCmd(graph, node)
			: null;
	}

	@Override
	protected void refreshNodeVisuals(VarNode node, IFigure figure) {
		if (figure instanceof StockFigure f) {
			f.setVar(node.variable());
		} else if (figure instanceof AuxFigure f) {
			f.setVar(node.variable());
		} else if (figure instanceof FlowFigure f) {
			f.setVar(node.variable());
		}
	}

}
