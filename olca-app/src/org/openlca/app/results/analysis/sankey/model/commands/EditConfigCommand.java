package org.openlca.app.results.analysis.sankey.model.commands;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.results.analysis.sankey.SankeyConfig;
import org.openlca.app.results.analysis.sankey.SankeyEditor;
import org.openlca.app.results.analysis.sankey.model.Diagram;

import static org.openlca.app.results.analysis.sankey.SankeyConfig.CONFIG_PROP;

public class EditConfigCommand extends Command {

	private final SankeyEditor editor;
	private final GraphicalViewer viewer;
	private final SankeyConfig oldConfig;
	private final SankeyConfig newConfig;
	/** Stores the new diagram. */
	private Diagram newDiagram;
	/** Stores the old diagram. */
	private final Diagram oldDiagram;

	/**
	 * Create a command that can reset the Graph model to use the new config.
	 */
	public EditConfigCommand(Diagram diagram, SankeyConfig newConfig) {
		if (diagram == null) {
			throw new IllegalArgumentException();
		}
		this.editor = diagram.editor;
		this.viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
		this.oldDiagram = diagram;
		this.oldConfig = diagram.getConfig();
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
		newDiagram = editor.getSankeyFactory().createDiagram();

		editor.setModel(newDiagram);
		if (editor.getHeader() != null) {
			editor.getHeader().setModel(newDiagram);
			newDiagram.firePropertyChange(CONFIG_PROP, null, newDiagram.getConfig());
		}
		viewer.setContents(newDiagram);
	}

	@Override
	public void redo() {
		newConfig.copyTo(editor.config);
		editor.setModel(newDiagram);
		viewer.setContents(newDiagram);
	}

	@Override
	public void undo() {
		oldConfig.copyTo(editor.config);
		editor.setModel(oldDiagram);
		viewer.setContents(oldConfig);
	}

}
