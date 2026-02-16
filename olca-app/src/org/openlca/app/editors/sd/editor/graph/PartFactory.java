package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.openlca.app.components.graphics.themes.Theme;

class PartFactory implements EditPartFactory {

	private final Theme theme;

	PartFactory(Theme theme) {
		this.theme = theme;
	}

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		return switch (model) {
			case GraphModel m -> new GraphPart(m, theme);
			case VarModel m -> new VarPart(m, theme);
			case LinkModel m -> new LinkPart(m, theme);
			case null, default -> null;
		};
	}

}
