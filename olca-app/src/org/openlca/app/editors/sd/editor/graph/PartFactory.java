package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SdVarLink;
import org.openlca.app.editors.sd.editor.graph.model.SdVarNode;

class PartFactory implements EditPartFactory {

	private final Theme theme;

	PartFactory(Theme theme) {
		this.theme = theme;
	}

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		return switch (model) {
			case SdGraph m -> new GraphPart(m, theme);
			case SdVarNode m -> new VarPart(m, theme);
			case SdVarLink m -> new LinkPart(m, theme);
			case null, default -> null;
		};
	}

}
