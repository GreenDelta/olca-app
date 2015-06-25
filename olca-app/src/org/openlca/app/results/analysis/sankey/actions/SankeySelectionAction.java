package org.openlca.app.results.analysis.sankey.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;
import org.openlca.app.results.analysis.sankey.SankeySelectionDialog;
import org.openlca.core.results.FullResultProvider;

/**
 * Opens the {@link SankeySelectionDialog} and updates the Sankey diagram with
 * the selection from this dialog.
 */
public class SankeySelectionAction extends Action {

	private Object lastSelection;
	private SankeyDiagram sankeyDiagram;

	@Override
	public String getText() {
		return Messages.SetSankeyDiagramOptions;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.SANKEY_OPTIONS_ICON.getDescriptor();
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
		FullResultProvider result = sankeyDiagram.getResult();
		if (result == null)
			return;
		openAndUpdate(result);
	}

	private void openAndUpdate(FullResultProvider result) {
		SankeySelectionDialog dialog = new SankeySelectionDialog(result);
		dialog.setCutoff(sankeyDiagram.getModel().getCutoff());
		dialog.setSelection(lastSelection);
		if (dialog.open() == Window.OK) {
			lastSelection = dialog.getSelection();
			if (lastSelection == null)
				return;
			sankeyDiagram.update(lastSelection, dialog.getCutoff());
		}
	}
}
