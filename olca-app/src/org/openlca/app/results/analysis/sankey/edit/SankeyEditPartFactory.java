package org.openlca.app.results.analysis.sankey.edit;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.openlca.app.results.analysis.sankey.model.Diagram;
import org.openlca.app.results.analysis.sankey.model.SankeyNode;
import org.openlca.app.tools.graphics.model.Link;

public class SankeyEditPartFactory implements EditPartFactory {

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		var part = editPartOf(model);
		if (part == null) {
			return null;
		}
		part.setModel(model);
		return part;
	}

	private EditPart editPartOf(Object model) {
		if (model instanceof Diagram)
			return new DiagramEditPart();
		else if (model instanceof SankeyNode)
			return new SankeyNodeEditPart();
		else if (model instanceof Link)
			return new LinkEditPart();
		else return null;
	}

}
