package org.openlca.app.editors.sd.editor.graph.edit;

import java.util.List;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.editpolicies.ResizableEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.app.editors.sd.editor.graph.model.NotifySupport;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SdNode;
import org.openlca.app.editors.sd.editor.graph.model.VarLink;

abstract sealed class NodePart<T extends SdNode>
	extends AbstractGraphicalEditPart
	implements NodeEditPart
	permits VarPart, SystemPart {

	protected final Theme theme;
	private final T node;

	private final Runnable onModelChange = () -> {
		refreshVisuals();
		refreshSourceConnections();
		refreshTargetConnections();
	};

	NodePart(T node, Theme theme) {
		setModel(node);
		this.node = node;
		this.theme = theme;
	}

	@Override
	public T getModel() {
		return node;
	}

	protected SdGraph getGraph() {
		var parent = getParent();
		return parent instanceof GraphPart gp
			? gp.getModel()
			: null;
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
			protected Command createDeleteCommand(GroupRequest req) {
				return NodePart.this.getDeleteCommand();
			}
		});
	}

	/// GEF sends a `REQ_OPEN` request when a figure is double-clicked (see
	/// `SelectEditPartTracker#performOpen`). We use this to open the edit
	/// dialog of the node that is represented by the clicked figure. If we
	/// cannot resolve the editor, we do nothing here.
	@Override
	public void performRequest(Request req) {
		if (req == null)
			return;
		if (req.getType() == RequestConstants.REQ_OPEN) {
			var editor = getEditor();
			if (editor != null) {
				openDialog(editor);
			}
			return;
		}
		super.performRequest(req);
	}

	/// The editor that contains this part. This is the editor part of the
	/// edit domain of the viewer that renders this part. It can be `null`
	/// if it cannot be resolved.
	private SdGraphEditor getEditor() {
		var viewer = getViewer();
		var domain = viewer != null ? viewer.getEditDomain() : null;
		return domain instanceof DefaultEditDomain d
			&& d.getEditorPart() instanceof SdGraphEditor e
			? e
			: null;
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
	protected List<VarLink> getModelSourceConnections() {
		var model = getModel();
		return model != null ? model.sourceLinks() : List.of();
	}

	@Override
	protected List<VarLink> getModelTargetConnections() {
		var model = getModel();
		return model != null ? model.targetLinks() : List.of();
	}

	@Override
	protected void refreshVisuals() {
		var node = getModel();
		if (node == null)
			return;

		refreshNodeVisuals(node, getFigure());

		var parent = getParent();
		if (parent instanceof AbstractGraphicalEditPart gep) {
			gep.setLayoutConstraint(this, getFigure(), node.bounds());
		}
	}

	protected abstract Command getDeleteCommand();

	protected abstract void refreshNodeVisuals(T node, IFigure figure);

	/// Opens the edit dialog of this node. This is called when the figure of
	/// this node is double-clicked in the graphical editor.
	protected abstract void openDialog(SdGraphEditor editor);
}
