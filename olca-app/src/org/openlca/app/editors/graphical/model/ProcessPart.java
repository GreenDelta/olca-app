package org.openlca.app.editors.graphical.model;

import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.GroupRequest;
import org.openlca.app.editors.graphical.command.CommandUtil;
import org.openlca.app.editors.graphical.command.DeleteProcessCommand;
import org.openlca.app.editors.graphical.command.XYLayoutCommand;
import org.openlca.app.editors.graphical.policy.LayoutPolicy;

public class ProcessPart extends AbstractNodeEditPart<ProcessNode> {

	@Override
	protected void addChildVisual(EditPart childEditPart, int index) {
		super.addChildVisual(childEditPart, getContentPane().getChildren().size());
	}

	@Override
	protected IFigure createFigure() {
		ProcessNode node = getModel();
		ProcessFigure figure = new ProcessFigure(node);
		node.setFigure(figure);
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new LayoutPolicy());
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ComponentEditPolicy() {
			@Override
			protected Command createDeleteCommand(GroupRequest req) {
				return new DeleteProcessCommand(getModel());
			}
		});
	}

	@Override
	public ProductSystemPart getParent() {
		return (ProductSystemPart) super.getParent();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<IOPart> getChildren() {
		return super.getChildren();
	}

	@Override
	public Command getCommand(Request req) {
		if (!(req instanceof ChangeBoundsRequest))
			return super.getCommand(req);
		ChangeBoundsRequest request = (ChangeBoundsRequest) req;
		Command commandChain = null;
		for (Object part : request.getEditParts()) {
			if (!(part instanceof ProcessPart))
				continue;
			Command command = getCommand((ProcessPart) part, request);
			commandChain = CommandUtil.chain(command, commandChain);
		}
		return commandChain;
	}

	private Command getCommand(ProcessPart part, ChangeBoundsRequest request) {
		IFigure figure = part.getModel().figure;
		Rectangle bounds = figure.getBounds().getCopy();
		figure.translateToAbsolute(bounds);
		Rectangle moveResize = new Rectangle(request.getMoveDelta(), request.getSizeDelta());
		bounds.resize(moveResize.getSize());
		bounds.translate(moveResize.getLocation());
		figure.translateToRelative(bounds);
		if (request.getSizeDelta().height != 0 || request.getSizeDelta().width != 0)
			return XYLayoutCommand.resize(part.getModel(), bounds);
		if (request.getMoveDelta().x != 0 || request.getMoveDelta().y != 0)
			return XYLayoutCommand.move(part.getModel(), bounds);
		return null;
	}

	@Override
	protected List<Link> getModelSourceConnections() {
		return getModel().links;
	}

	@Override
	protected List<Link> getModelTargetConnections() {
		return getModel().links;
	}

	@Override
	public boolean isSelectable() {
		return true;
	}

	@Override
	public void setSelected(int value) {
		if (getFigure().isVisible()) {
			super.setSelected(value);
			for (Link link : getModel().links) {
				if (!link.isVisible())
					continue;
				link.setSelected(value);
			}
		}
	}

	@Override
	public void refreshSourceConnections() {
		// make public
		super.refreshSourceConnections();
	}

	@Override
	public void refreshTargetConnections() {
		// make public
		super.refreshTargetConnections();
	}

	@Override
	public void refresh() {
		super.refresh();
	}

	@Override
	protected void refreshVisuals() {
		// we recreate the IO node here when the
		// display of elementary flows changed.
		var thisNode = getModel();
		var childs = thisNode.getChildren();
		if (childs.isEmpty())
			return;
		var withElems = thisNode.config().showElementaryFlows;
		var ioNode = childs.get(0);
		if (ioNode.isWithElementaryFlows == withElems)
			return;
		thisNode.remove(ioNode);
		thisNode.add(new IONode(thisNode));
		super.refreshVisuals();
	}
}
