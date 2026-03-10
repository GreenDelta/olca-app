package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.editpolicies.ResizableEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.editors.sd.editor.graph.model.NotifySupport;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SdVarLink;
import org.openlca.app.editors.sd.editor.graph.model.SdVarNode;
import org.openlca.app.editors.sd.editor.graph.view.AuxFigure;
import org.openlca.app.editors.sd.editor.graph.view.FlowFigure;
import org.openlca.app.editors.sd.editor.graph.view.StockFigure;
import org.openlca.sd.model.Auxil;
import org.openlca.sd.model.Rate;

import java.util.List;

public class VarPart extends AbstractGraphicalEditPart implements NodeEditPart {

	private final Theme theme;
	private final Runnable onModelChange = () -> {
		refreshVisuals();
		refreshSourceConnections();
		refreshTargetConnections();
	};

	VarPart(SdVarNode model, Theme theme) {
		setModel(model);
		this.theme = theme;
	}

	@Override
	protected IFigure createFigure() {
		var model = getModel();
		if (model == null) return new StockFigure(theme);
		return switch (getModel().variable()) {
			case Auxil ignore -> new AuxFigure(theme);
			case Rate ignore -> new FlowFigure(theme);
			case null, default -> new StockFigure(theme);
		};
	}

	@Override
	public SdVarNode getModel() {
		var model = super.getModel();
		return model instanceof SdVarNode m ? m : null;
	}

	@Override
	public void activate() {
		super.activate();
		NotifySupport.on(getModel(), onModelChange);
	}

	@Override
	public void deactivate() {
		NotifySupport.off(getModel(), onModelChange);
		super.deactivate();
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.PRIMARY_DRAG_ROLE, new ResizableEditPolicy());
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ComponentEditPolicy() {
			@Override
			protected Command createDeleteCommand(GroupRequest request) {
				var node = getModel();
				var parent = getParent();
				if (!(parent instanceof GraphPart graphPart))
					return null;
				SdGraph graph = graphPart.getModel();
				return node != null && graph != null
					? new DeleteVarCmd(graph, node)
					: null;
			}
		});
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart con) {
		return new ChopboxAnchor(getFigure());
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart con) {
		return new ChopboxAnchor(getFigure());
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request request) {
		return new ChopboxAnchor(getFigure());
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(Request request) {
		return new ChopboxAnchor(getFigure());
	}

	@Override
	protected List<SdVarLink> getModelSourceConnections() {
		var model = getModel();
		return model != null ? model.sourceLinks() : List.of();
	}

	@Override
	protected List<SdVarLink> getModelTargetConnections() {
		var model = getModel();
		return model != null ? model.targetLinks() : List.of();
	}

	@Override
	protected void refreshVisuals() {
		var node = getModel();
		if (node == null)
			return;
		var figure = getFigure();
		if (figure instanceof StockFigure f) {
			f.setVar(node.variable());
		} else if (figure instanceof AuxFigure f) {
			f.setVar(node.variable());
		} else if (figure instanceof FlowFigure f) {
			f.setVar(node.variable());
		}

		var parent = getParent();
		if (parent instanceof AbstractGraphicalEditPart gep) {
			gep.setLayoutConstraint(this, figure, node.bounds());
		}
	}

}
