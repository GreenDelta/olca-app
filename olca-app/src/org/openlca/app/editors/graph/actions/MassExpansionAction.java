package org.openlca.app.editors.graph.actions;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.StackAction;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.editors.graph.GraphEditor;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.editors.graph.requests.ExpansionRequest;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Question;

import static org.openlca.app.editors.graph.requests.GraphRequestConstants.*;

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
			setText(M.ExpandAll + " && " + NLS.bind(M.LayoutAs, M.Tree));
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
			var request = new ExpansionRequest(referenceNode, REQ_COLLAPSE);
			cc.add(editPart.getCommand(request));
		}

		else if (type == EXPAND) {
			for (var node : editor.getModel().getChildren()) {
				if (!node.isExpanded(Node.Side.INPUT)
					|| !node.isExpanded(Node.Side.OUTPUT)) {
					var editPart = (EditPart) viewer.getEditPartRegistry().get(node);
					var request = new ExpansionRequest(node, REQ_EXPAND);
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

		if (doIt) {
			execute(getCommand());

			// The layout command has to be executed after creating or deleting the
			// nodes, hence cannot be executed within a CompoundCommand.
			var view = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
			if (view == null) {
				return;
			}
			var graphEditPart = (EditPart) view.getEditPartRegistry()
				.get(editor.getModel());
			var layoutCommand = graphEditPart.getCommand(new Request(REQ_LAYOUT));
			if (layoutCommand.canExecute()) {
				var stack = (CommandStack) editor.getAdapter(CommandStack.class);
				stack.execute(layoutCommand);
			}
		}
		System.out.println("Number of nodes: " + editor.getModel().getChildren().size());
	}

}
