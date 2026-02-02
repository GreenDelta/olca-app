package org.openlca.app.editors.graphical.actions;

import static org.openlca.app.editors.graphical.requests.GraphRequests.*;

import java.util.HashMap;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.editors.graphical.GraphActionIds;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.ExchangeEditPart;
import org.openlca.app.editors.graphical.edit.LinkEditPart;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.rcp.images.Icon;
import org.openlca.util.Exchanges;

public class RemoveChainAction extends SelectionAction {

	public static final String KEY_ROOT = "root";
	private final GraphEditor editor;

	public RemoveChainAction(GraphEditor editor) {
		super(editor);
		this.editor = editor;
		setId(GraphActionIds.REMOVE_CHAIN);
		setText("Remove process chain");
		setImageDescriptor(Icon.REMOVE_SUPPLY_CHAIN.descriptor());
	}

	@Override
	protected boolean calculateEnabled() {
		var command = getCommand();
		return command != null && command.canExecute();
	}

	@Override
	public void run() {
		var cmd = getCommand();
		if (cmd != null) {
			execute(cmd);
		}
	}

	private Command getCommand() {
		var root = getRoot();
		if (root == null) return null;
		var graph = editor.getEditPartOf(editor.getModel());
		if (graph == null) return null;

		var request = new Request(REQ_REMOVE_CHAIN);
		var data = new HashMap<String, Object>();
		data.put(KEY_ROOT, root);
		request.setExtendedData(data);
		return graph.getCommand(request);
	}

	/// Get the selected node or link on which this action was called.
	private Object getRoot() {
		var selection = getSelectedObjects();
		if (selection == null || selection.isEmpty()) return null;
		var object = selection.getFirst();

		return switch (object) {
			case NodeEditPart p -> p.getModel();
			case LinkEditPart p -> p.getModel();
			case ExchangeEditPart p -> {
				var item = p.getModel();
				// deleting a process chain for a selected exchange makes only
				// sense for linked product inputs or waste outputs
				if (item == null || !Exchanges.isLinkable(item.exchange)) {
					yield null;
				}
				var cons = p.getModel().getAllConnections();
				yield cons.size() == 1
					? cons.getFirst()
					: null;
			}
			case null, default -> null;
		};
	}
}
