package org.openlca.app.results.analysis.sankey.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;
import org.openlca.app.results.analysis.sankey.SankeySelectionDialog;
import org.openlca.core.results.FullResult;

/**
 * Opens the {@link SankeySelectionDialog} and updates the Sankey diagram with
 * the selection from this dialog.
 */
public class SankeySelectionAction extends Action {

	private Object lastSelection;
	private SankeyDiagram sankeyDiagram;

	@Override
	public String getText() {
		return M.SetSankeyDiagramOptions;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.SANKEY_OPTIONS.descriptor();
	}

	public void setSankeyDiagram(SankeyDiagram sankeyDiagram) {
		if (sankeyDiagram == this.sankeyDiagram)
			return;
		this.sankeyDiagram = sankeyDiagram;
		if (sankeyDiagram != null)
			lastSelection = sankeyDiagram.getDefaultSelection();
	}

	@Override
	public void run() {
		if (sankeyDiagram == null)
			return;
		FullResult result = sankeyDiagram.result;
		if (result == null)
			return;
		openAndUpdate(result);
	}

	private void openAndUpdate(FullResult result) {
		SankeySelectionDialog dialog = new SankeySelectionDialog(result);
		dialog.cutoff = sankeyDiagram.node.cutoff;
		dialog.selection = lastSelection;
		if (dialog.open() == Window.OK) {
			lastSelection = dialog.selection;
			if (lastSelection == null)
				return;
			sankeyDiagram.update(lastSelection, dialog.cutoff);
		}
	}
}
