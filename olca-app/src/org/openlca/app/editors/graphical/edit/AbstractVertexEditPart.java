package org.openlca.app.editors.graphical.edit;

import java.beans.PropertyChangeEvent;
import java.util.List;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.openlca.app.editors.graphical.model.*;
import org.openlca.app.editors.graphical.model.commands.CreateLinkCommand;

/**
 * This class abstract the creation of a graph component that can be linked with
 * <code>Link</code>s. Usually called AbstractNodeEditPart, we reserve that name
 * to the EditPart of a <code>Node</code>.
 *
 * @param <N> The type of the model element.
 */
public abstract class AbstractVertexEditPart<N extends GraphComponent> extends
	AbstractComponentEditPart<N> implements NodeEditPart {

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (GraphComponent.TARGET_CONNECTIONS_PROP.equals(prop))
			refreshTargetConnections();
		else if (GraphComponent.SOURCE_CONNECTIONS_PROP.equals(prop))
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
		return new LinkAnchor(getFigure(), false);
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request req) {
		if (req instanceof CreateConnectionRequest)
			return sourceAnchor((CreateConnectionRequest) req);
		if (req instanceof ReconnectRequest)
			return sourceAnchor((ReconnectRequest) req);
		return null;
	}

	private ConnectionAnchor sourceAnchor(CreateConnectionRequest req) {
		var command = req.getStartCommand();
		if (!(command instanceof CreateLinkCommand))
			return null;
		var cmd = (CreateLinkCommand) req.getStartCommand();
		if (cmd.source != null) {
			return new LinkAnchor(getOwner(cmd.source), false);
		}
		if (cmd.target != null) {
			return new LinkAnchor(getOwner(cmd.target), true);
		}
		return null;
	}

	private ConnectionAnchor sourceAnchor(ReconnectRequest req) {
		Link link = (Link) req.getConnectionEditPart().getModel();
		Node node = ((ExchangeEditPart) req.getTarget()).getModel().getNode();
		var output = node.getOutput(link.processLink);
		var input = link.getTargetNode().getInput(link.processLink);
		if (input == null || !input.matches(output))
			return null;
		return new LinkAnchor(getOwner(output), false);
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart con) {
		return new LinkAnchor(getFigure(), true);
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(Request req) {
		if (req instanceof CreateConnectionRequest)
			return targetAnchor((CreateConnectionRequest) req);
		if (req instanceof ReconnectRequest)
			return targetAnchor((ReconnectRequest) req);
		return null;
	}

	private ConnectionAnchor targetAnchor(CreateConnectionRequest req) {
		CreateLinkCommand cmd = (CreateLinkCommand) req.getStartCommand();
		if (cmd.startedFromSource) {
			if (cmd.target != null)
				return new LinkAnchor(getOwner(cmd.target), true);
			return null;
		}
		if (cmd.source != null)
			return new LinkAnchor(getOwner(cmd.source), false);
		return null;
	}

	private ConnectionAnchor targetAnchor(ReconnectRequest req) {
		Link link = (Link) req.getConnectionEditPart().getModel();
		var input = ((ExchangeEditPart) req.getTarget()).getModel();
		var output = link.getSourceNode().getInput(link.processLink);
		if (output == null || !output.matches(input))
			return null;

		// TODO: waste links
		if (input.exchange.id != link.processLink.exchangeId
			&& input.isConnected())
			return null;

		return new LinkAnchor(getOwner(input), true);
	}

	private IFigure getOwner(ExchangeItem item) {
		var editPartRegistry = getViewer().getEditPartRegistry();
		var itemEditPart = (ExchangeEditPart) editPartRegistry.get(item);
		return itemEditPart.getFigure();
	}

}
