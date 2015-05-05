package org.openlca.app.results.analysis.sankey.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.swt.graphics.Font;
import org.openlca.app.db.Cache;
import org.openlca.app.results.analysis.sankey.layout.LayoutPolicy;
import org.openlca.app.results.analysis.sankey.layout.XYLayoutCommand;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

public class ProcessEditPart extends AbstractGraphicalEditPart implements
		NodeEditPart, PropertyChangeListener {

	private EntityCache cache = Cache.getEntityCache();

	@Override
	public void activate() {
		super.activate();
		((Node) getModel()).addPropertyChangeListener(this);
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new LayoutPolicy());
	}

	@Override
	protected IFigure createFigure() {
		ProcessNode process = getModel();
		ProcessFigure figure = new ProcessFigure(process);
		figure.addPropertyChangeListener(this);
		return figure;
	}

	@Override
	public ProcessNode getModel() {
		return (ProcessNode) super.getModel();
	}

	@Override
	protected List<ConnectionLink> getModelSourceConnections() {
		List<ConnectionLink> sourceConnections = new ArrayList<>();
		ProcessNode thisNode = getModel();
		for (ConnectionLink link : thisNode.getLinks()) {
			if (link.getSourceNode().equals(thisNode))
				sourceConnections.add(link);
		}
		return sourceConnections;
	}

	@Override
	protected List<ConnectionLink> getModelTargetConnections() {
		List<ConnectionLink> targetConnections = new ArrayList<>();
		for (ConnectionLink link : getModel().getLinks()) {
			if (link.getTargetNode().equals(getModel())) {
				targetConnections.add(link);
			}
		}
		return targetConnections;
	}

	@Override
	public void deactivate() {
		IFigure figure = getFigure();
		if (figure instanceof ProcessFigure) {
			ProcessFigure pFigure = (ProcessFigure) figure;
			Font boldFont = pFigure.getBoldFont();
			if (boldFont != null && !boldFont.isDisposed())
				boldFont.dispose();
		}
		super.deactivate();
		((Node) getModel()).removePropertyChangeListener(this);
	}

	@Override
	public Command getCommand(Request request) {
		Command requested = null;
		if (request instanceof ChangeBoundsRequest) {
			ChangeBoundsRequest req = (ChangeBoundsRequest) request;
			if (req.getSizeDelta().height == 0 && req.getSizeDelta().width == 0) {
				Command commandChain = null;
				for (Object o : req.getEditParts()) {
					if (o instanceof ProcessEditPart) {
						ProcessEditPart part = (ProcessEditPart) o;
						XYLayoutCommand command = new XYLayoutCommand();
						command.setProcessNode(part.getModel());

						Rectangle bounds = (part.getModel()).getFigure()
								.getBounds().getCopy();
						part.getModel().getFigure().translateToAbsolute(bounds);
						Rectangle moveResize = new Rectangle(
								req.getMoveDelta(), req.getSizeDelta());
						bounds.resize(moveResize.getSize());
						bounds.translate(moveResize.getLocation());
						part.getModel().getFigure().translateToRelative(bounds);
						command.setConstraint(bounds);
						if (commandChain == null) {
							commandChain = command;
						} else {
							commandChain = commandChain.chain(command);
						}
					}
				}
				requested = commandChain;
			}
		}
		return requested;
	}

	@Override
	public List<Node> getModelChildren() {
		return getModel().getChildrenArray();
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(
			ConnectionEditPart connection) {
		ConnectionLink link = (ConnectionLink) connection.getModel();
		return new ProcessLinkAnchor(link, false);
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request arg0) {
		return null;
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(
			ConnectionEditPart connection) {
		ConnectionLink link = (ConnectionLink) connection.getModel();
		return new ProcessLinkAnchor(link, true);
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(Request arg0) {
		return null;
	}

	@Override
	public boolean isSelectable() {
		return true;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(ProcessNode.CONNECTION))
			refreshConnections(evt);
		else {
			GraphicalEditPart part = (GraphicalEditPart) getViewer()
					.getContents();
			IFigure figure = part.getFigure();
			figure.revalidate();
		}
	}

	private void refreshConnections(PropertyChangeEvent evt) {
		Object linkObj = evt.getOldValue() != null ? evt.getOldValue() : evt
				.getNewValue();
		if (!(linkObj instanceof ConnectionLink))
			return;
		ConnectionLink link = (ConnectionLink) linkObj;
		ProcessDescriptor thisProcess = getModel().getProcess();

		ProcessDescriptor provider = cache.get(ProcessDescriptor.class, link
				.getProcessLink().getProviderId());
		ProcessDescriptor recipient = cache.get(ProcessDescriptor.class, link
				.getProcessLink().getRecipientId());
		boolean isLoop = Objects.equal(provider, recipient);
		try {
			if (thisProcess.equals(provider)) {
				refreshSourceConnections();
				if (isLoop)
					refreshTargetConnections();
			} else {
				refreshTargetConnections();
			}
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Failed to refresh connections for process "
					+ thisProcess, e);
		}
	}
}
