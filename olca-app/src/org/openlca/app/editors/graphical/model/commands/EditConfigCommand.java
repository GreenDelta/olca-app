package org.openlca.app.editors.graphical.model.commands;

import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.GraphFile;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.jsonld.Json;

public class EditConfigCommand extends Command {

	private final Graph graph;
	private final GraphEditor editor;
	/** Stores the new nodes. */
	private List<Node> newChildren;
	/** Stores the old nodes. */
	private final List<Node> oldChildren;
	private final GraphConfig oldConfig;
	private final GraphConfig newConfig;

	/**
	 * Create a command that can reset the location of all the nodes to force a
	 * complete relayout.
	 */
	public EditConfigCommand(Graph graph, GraphConfig newConfig) {
		if (graph == null) {
			throw new IllegalArgumentException();
		}
		this.graph = graph;
		this.editor = graph.editor;
		this.oldChildren = graph.getChildren();
		this.oldConfig = graph.getConfig();
		this.newConfig = newConfig;

		setLabel(NLS.bind(M.Edit, M.Settings));
	}

	@Override
	public boolean canExecute() {
		return true;
	}

	@Override
	public void execute() {
		newConfig.copyTo(editor.config);
		// Create new nodes with the new config.
		var rootObj = GraphFile.createJsonArray(editor, editor.getModel());
		var array = Json.getArray(rootObj, "nodes");
		var newGraph = editor.getGraphFactory().createGraph(editor, array);
		newChildren = newGraph.getChildren();
		redo();
	}

	@Override
	public void redo() {
		editor.getModel().removeAllChildren();
		editor.getModel().addChildren(newChildren);
	}

	@Override
	public void undo() {
		oldConfig.copyTo(editor.config);
		editor.getModel().removeAllChildren();
		editor.getModel().addChildren(oldChildren);
	}

}
