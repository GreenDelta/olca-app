package org.openlca.app.results.analysis.sankey.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;
import org.openlca.app.results.analysis.sankey.SankeySelectionDialog;

/**
 * Opens the {@link SankeySelectionDialog} and updates the Sankey diagram with
 * the selection from this dialog.
 */
public class SankeySelectionAction extends Action {

	private final SankeyDiagram editor;

	public SankeySelectionAction(SankeyDiagram editor) {
		this.editor = editor;
	}
	
	@Override
	public String getText() {
		return M.SetSankeyDiagramOptions;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.SANKEY_OPTIONS.descriptor();
	}

	@Override
	public void run() {
		if (editor == null)
			return;
		var d = new SankeySelectionDialog(editor);
		if (d.open() == Window.OK) {
			editor.update(d.selection, d.cutoff, d.maxCount);
		}
	}
}
