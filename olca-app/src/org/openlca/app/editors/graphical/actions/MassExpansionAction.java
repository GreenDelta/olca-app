package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.StackAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.requests.ExpandCollapseRequest;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Question;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.*;

/**
 * <p>
 * This action is of two type:
 * 	<ul>
 * 	  <li>Expand all the extremity nodes of the supply chain (the first providers and the
 *    last recipients).</li>
 *    <li>Collapse all the input and output side of the reference node.</li>
 *  </ul>
 * </p>
 */
public class MassExpansionAction extends StackAction {

	static final int NODE_LIMITATION = 250;
	public static final int EXPAND = 1;
	public static final int COLLAPSE = 2;
	private final int type;
	private final GraphEditor editor;

	public MassExpansionAction(GraphEditor part, int type) {
		super(part);
		editor = part;
		if (type == EXPAND) {
			setId(ActionIds.EXPAND_ALL);
			setText(M.ExpandAll);
			setImageDescriptor(Icon.EXPAND.descriptor());
		} else if (type == COLLAPSE) {
			setId(ActionIds.COLLAPSE_ALL);
			setText(M.CollapseAll);
			setImageDescriptor(Icon.COLLAPSE.descriptor());
		}
		this.type = type;
	}

	@Override
	protected boolean calculateEnabled() {
		var command = getCommand();
		if (command == null)
			return false;
		return command.canExecute();
	}

	private Command getCommand() {
		var cc = new CompoundCommand();
		cc.setDebugLabel("Mass " + (type == COLLAPSE ? "collapse" : "expansion"));

		if (editor == null || editor.getModel() == null)
			return null;

		var viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
		if (viewer == null)
			return null;

		if (type == COLLAPSE) {
			var referenceNode = editor.getModel().getReferenceNode();

			var editPart = (EditPart) viewer.getEditPartRegistry().get(referenceNode);
			var request = new ExpandCollapseRequest(referenceNode, REQ_COLLAPSE);
			if (editPart != null)
				cc.add(editPart.getCommand(request));
		}

		else if (type == EXPAND) {
			for (var node : editor.getModel().getChildren()) {
				if (!node.isExpanded(Node.Side.INPUT)
					|| !node.isExpanded(Node.Side.OUTPUT)) {
					var editPart = (EditPart) viewer.getEditPartRegistry().get(node);
					var request = new ExpandCollapseRequest(node, REQ_EXPAND);
					if (editPart != null)
						cc.add(editPart.getCommand(request));
				}
			}
		}

		return cc.unwrap();
	}

	@Override
	public void run() {
		// Ask if the model is very large.
		int count = editor.getModel().getChildren().size();
		var doIt = type == COLLAPSE || count < NODE_LIMITATION || Question.ask(
			M.ExpandAll, M.ExpandAll + ": " + count + " " + M.Processes);

		if (doIt)	execute(getCommand());
	}

}
