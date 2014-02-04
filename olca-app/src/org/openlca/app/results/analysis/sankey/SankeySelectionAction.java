package org.openlca.app.results.analysis.sankey;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.core.results.FullResultProvider;

/**
 * Opens the {@link SankeySelectionDialog} and updates the Sankey diagram with
 * the selection from this dialog.
 */
public class SankeySelectionAction extends Action {

	private double cutoff = 0.1;
	private Object lastSelection;
	private SankeyDiagram sankeyDiagram;

	@Override
	public String getText() {
		return Messages.Sankey_ActionText;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.SANKEY_OPTIONS_ICON.getDescriptor();
	}

	public void setSankeyDiagram(SankeyDiagram sankeyDiagram) {
		if (this.sankeyDiagram != sankeyDiagram) {
			this.sankeyDiagram = sankeyDiagram;
			lastSelection = null;
		}
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
		dialog.setCutoff(cutoff);
		dialog.setSelection(lastSelection);
		if (dialog.open() == Window.OK) {
			lastSelection = dialog.getSelection();
			if (lastSelection == null)
				return;
			cutoff = dialog.getCutoff();
			sankeyDiagram.update(lastSelection, cutoff);
		}
	}
}
