package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

class PartFactory implements EditPartFactory {

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		if (model instanceof GraphModel) {
			var part = new GraphPart();
			part.setModel(model);
			return part;
		}
		if (model instanceof VarModel) {
			var part = new VarPart();
			part.setModel(model);
			return part;
		}
		return null;
	}

}
