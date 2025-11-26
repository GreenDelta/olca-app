package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.Action;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.app.editors.sd.editor.graph.model.SdNode;
import org.openlca.app.editors.sd.editor.graph.model.SdNodeType;
import org.openlca.app.editors.sd.editor.graph.model.commands.CreateNodeCommand;
import org.openlca.app.rcp.images.Icon;

/**
 * Action to add a Stock node to the graph.
 */
public class AddStockAction extends Action {

	public static final String ID = "sd.graph.addStock";

	private final SdGraphEditor editor;
	private static int stockCounter = 1;

	public AddStockAction(SdGraphEditor editor) {
		this.editor = editor;
		setId(ID);
		setText("Add Stock");
		setToolTipText("Add a new stock variable");
		setImageDescriptor(Icon.ADD.descriptor());
	}

	@Override
	public void run() {
		var graph = editor.getModel();
		var name = "Stock_" + stockCounter++;
		var node = new SdNode(SdNodeType.STOCK, name);

		// Place the new node at a default position
		var bounds = new Rectangle(new Point(100, 100), node.getDefaultSize());
		Command cmd = new CreateNodeCommand(graph, node, bounds);
		editor.getRootEditPart().getViewer().getEditDomain().getCommandStack().execute(cmd);

		// TODO: Bind to actual SD model variable
		editor.setDirty();
	}
}
