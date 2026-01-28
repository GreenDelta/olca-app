package org.openlca.app.editors.graphical.edit;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.IOPane;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.model.StickyNote;

public class GraphEditPartFactory implements EditPartFactory {

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
		return switch (model) {
			case Graph ignore -> new GraphEditPart();

			case Node node -> node.isMinimized()
				? new NodeEditPart.Minimized()
				: new NodeEditPart.Maximized();

			case StickyNote note -> note.isMinimized()
				? new StickyNoteEditPart.Minimized()
				: new StickyNoteEditPart.Maximized();

			case IOPane ignore -> new IOPaneEditPart();
			case ExchangeItem ignore -> new ExchangeEditPart();
			case GraphLink ignore -> new LinkEditPart();
			case null, default ->  null;
		};
	}

}
