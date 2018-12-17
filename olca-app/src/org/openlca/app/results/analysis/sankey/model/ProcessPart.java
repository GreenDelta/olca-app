package org.openlca.app.results.analysis.sankey.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
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
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

public class ProcessPart extends AbstractGraphicalEditPart implements
		NodeEditPart, PropertyChangeListener {

	private EntityCache cache = Cache.getEntityCache();

	@Override
	public void activate() {
		super.activate();
		((Node) getModel()).listeners.addPropertyChangeListener(this);
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new LayoutPolicy());
	}

	@Override
	protected IFigure createFigure() {
		ProcessNode process = getModel();
		process.editPart = this;
		ProcessFigure figure = new ProcessFigure(process);
		figure.addPropertyChangeListener(this);
		return figure;
	}

	@Override
	public ProcessNode getModel() {
		return (ProcessNode) super.getModel();
	}

	@Override
	protected List<Link> getModelSourceConnections() {
		List<Link> sourceConnections = new ArrayList<>();
		ProcessNode thisNode = getModel();
		for (Link link : thisNode.links) {
			if (link.sourceNode.equals(thisNode))
				sourceConnections.add(link);
		}
		return sourceConnections;
	}

	@Override
	protected List<Link> getModelTargetConnections() {
		List<Link> targetConnections = new ArrayList<>();
		for (Link link : getModel().links) {
			if (link.targetNode.equals(getModel())) {
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
			Font boldFont = pFigure.boldFont;
			if (boldFont != null && !boldFont.isDisposed())
				boldFont.dispose();
		}
		super.deactivate();
		((Node) getModel()).listeners.removePropertyChangeListener(this);
	}

	@Override
	public Command getCommand(Request request) {
		if (!(request instanceof ChangeBoundsRequest))
			return null;
		ChangeBoundsRequest req = (ChangeBoundsRequest) request;
		Dimension sizeDelta = req.getSizeDelta();
		if (sizeDelta.height != 0 || sizeDelta.width != 0)
			return null;
		Command commandChain = null;
		for (Object o : req.getEditParts()) {
			if (!(o instanceof ProcessPart))
				continue;
			ProcessPart part = (ProcessPart) o;
			XYLayoutCommand command = new XYLayoutCommand();
			command.setProcessNode(part.getModel());
			Rectangle bounds = (part.getModel()).figure.getBounds().getCopy();
			part.getModel().figure.translateToAbsolute(bounds);
			Rectangle moveResize = new Rectangle(req.getMoveDelta(), sizeDelta);
			bounds.resize(moveResize.getSize());
			bounds.translate(moveResize.getLocation());
			part.getModel().figure.translateToRelative(bounds);
			command.setConstraint(bounds);
			if (commandChain == null) {
				commandChain = command;
			} else {
				commandChain = commandChain.chain(command);
			}
		}
		return commandChain;
	}

	@Override
	public List<Node> getModelChildren() {
		return getModel().children;
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connection) {
		Link link = (Link) connection.getModel();
		return new ProcessLinkAnchor(link, false);
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request arg0) {
		return null;
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection) {
		Link link = (Link) connection.getModel();
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
	public void setSelected(int value) {
		if (!getFigure().isVisible())
			return;
		super.setSelected(value);
		for (Link link : getModel().links) {
			if (!link.isVisible())
				continue;
			link.setSelected(value);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(ProcessNode.CONNECTION))
			refreshConnections(evt);
		else {
			GraphicalEditPart part = (GraphicalEditPart) getViewer().getContents();
			IFigure figure = part.getFigure();
			figure.revalidate();
		}
	}

	private void refreshConnections(PropertyChangeEvent evt) {
		Object linkObj = evt.getOldValue() != null ? evt.getOldValue() : evt.getNewValue();
		if (!(linkObj instanceof Link))
			return;
		Link link = (Link) linkObj;
		CategorizedDescriptor thisProcess = getModel().process;
		// TODO: we could have product systems here
		ProcessDescriptor provider = cache.get(ProcessDescriptor.class, link.processLink.providerId);
		ProcessDescriptor recipient = cache.get(ProcessDescriptor.class, link.processLink.processId);
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
			log.error("Failed to refresh connections for process " + thisProcess, e);
		}
	}
}
