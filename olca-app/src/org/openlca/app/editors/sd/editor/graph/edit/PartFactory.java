package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SystemNode;
import org.openlca.app.editors.sd.editor.graph.model.VarLink;
import org.openlca.app.editors.sd.editor.graph.model.VarNode;

public class PartFactory implements EditPartFactory {

	private final Theme theme;

	public PartFactory(Theme theme) {
		this.theme = theme;
	}

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		return switch (model) {
			case SdGraph m -> new GraphPart(m, theme);
			case SystemNode m -> new SystemPart(m, theme);
			case VarNode m -> new VarPart(m, theme);
			case VarLink m -> new LinkPart(m, theme);
			case null, default -> null;
		};
	}

}
