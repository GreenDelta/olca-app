package org.openlca.app.results.analysis.sankey.edit;

import java.beans.PropertyChangeEvent;
import java.util.List;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.openlca.app.results.analysis.sankey.model.SankeyLink;
import org.openlca.app.tools.graphics.model.Component;
import org.openlca.app.tools.graphics.model.Link;

import static org.openlca.app.tools.graphics.model.Component.SOURCE_CONNECTIONS_PROP;
import static org.openlca.app.tools.graphics.model.Component.TARGET_CONNECTIONS_PROP;


/**
 * This class abstract the creation of a graph component that can be linked with
 * <code>Link</code>s. Usually called AbstractNodeEditPart, we reserve that name
 * to the EditPart of a <code>Node</code>.
 *
 * @param <N> The type of the model element.
 */
public abstract class AbstractVertexEditPart<N extends Component> extends
		AbstractComponentEditPart<N> implements NodeEditPart {

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (TARGET_CONNECTIONS_PROP.equals(prop))
			refreshTargetConnections();
		else if (SOURCE_CONNECTIONS_PROP.equals(prop))
			refreshSourceConnections();
		else super.propertyChange(evt);
	}

	@Override
	protected List<Link> getModelSourceConnections() {
		return getModel().getSourceConnections();
	}

	@Override
	protected List<Link> getModelTargetConnections() {
		return getModel().getTargetConnections();
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart con) {
		var anchor = ((SankeyLink) con.getModel()).getSourceAnchor();
		return new LinkAnchor(getFigure(), false, anchor);
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request req) {
		return null;
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart con) {
		var anchor = ((SankeyLink) con.getModel()).getTargetAnchor();
		return new LinkAnchor(getFigure(), true, anchor);
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(Request req) {
		return null;
	}

}
