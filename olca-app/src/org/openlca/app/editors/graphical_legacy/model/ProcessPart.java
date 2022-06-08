package org.openlca.app.editors.graphical_legacy.model;

import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.openlca.app.editors.graphical_legacy.command.Commands;
import org.openlca.app.editors.graphical_legacy.command.XYLayoutCommand;
import org.openlca.app.editors.graphical_legacy.policy.LayoutPolicy;
import org.openlca.app.editors.graphical_legacy.policy.ProcessEditPolicy;
import org.openlca.app.editors.graphical_legacy.view.ProcessFigure;

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
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ProcessEditPolicy());
	}

	@Override
	public void performRequest(Request request) {
		if (request.getType() == RequestConstants.REQ_OPEN) {
			CommandStack stack = getViewer().getEditDomain().getCommandStack();
			var command = getCommand(request);
			stack.execute(command);
		}
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
		Command chain = null;
		for (Object part : request.getEditParts()) {
			if (!(part instanceof ProcessPart))
				continue;
			var command = moveOrResize((ProcessPart) part, request);
			if (command != null) {
				chain = Commands.chain(command, chain);
			}
		}
		return chain;
	}

	private Command moveOrResize(ProcessPart part, ChangeBoundsRequest req) {
		var node = part.getModel();
		var figure = part.getModel().figure;

		var bounds = figure.getBounds().getCopy();
		figure.translateToAbsolute(bounds);
		var sizeDelta = req.getSizeDelta();
		var moveDelta = req.getMoveDelta();
		bounds.resize(sizeDelta).translate(moveDelta);
		figure.translateToRelative(bounds);

		if (sizeDelta.height != 0 || sizeDelta.width != 0)
			return XYLayoutCommand.resize(node, bounds);
		if (moveDelta.x != 0 || moveDelta.y != 0)
			return XYLayoutCommand.move(node, bounds);
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
	public void refreshChildren() {
		var processNode = getModel();
		processNode.getChildren().clear();
		if (!processNode.isMinimized()) {
			processNode.getChildren().add(new IONode(processNode));
		}
		super.refreshChildren();
	}

}
