package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SdLink;
import org.openlca.app.editors.sd.editor.graph.model.SdNode;

/**
 * Factory that creates EditParts for the SD graph model elements.
 */
public class SdEditPartFactory implements EditPartFactory {

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		var part = createEditPartFor(model);
		if (part != null) {
			part.setModel(model);
		}
		return part;
	}

	private EditPart createEditPartFor(Object model) {
		if (model instanceof SdGraph) {
			return new SdGraphEditPart();
		} else if (model instanceof SdNode) {
			return new SdNodeEditPart();
		} else if (model instanceof SdLink) {
			return new SdLinkEditPart();
		}
		return null;
	}
}
