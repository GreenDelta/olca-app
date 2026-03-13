package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.commands.Command;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.editors.sd.editor.graph.model.SystemNode;
import org.openlca.app.editors.sd.editor.graph.view.SystemFigure;


public final class SystemPart extends NodePart<SystemNode> {

	SystemPart(SystemNode model, Theme theme) {
		super(model, theme);
	}

	@Override
	protected IFigure createFigure() {
		return new SystemFigure(theme);
	}

	@Override
	protected Command getDeleteCommand() {
		var graph = getGraph();
		var node = getModel();
		return graph != null && node != null
			? new SystemDeleteCmd(graph, node)
			: null;
	}

	@Override
	protected void refreshNodeVisuals(SystemNode node, IFigure figure) {
		if (figure instanceof SystemFigure f) {
			f.setBinding(node.binding());
		}
	}
}
