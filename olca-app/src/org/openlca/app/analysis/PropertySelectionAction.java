package org.openlca.app.analysis;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.openlca.app.Messages;
import org.openlca.app.analysis.sankey.SankeyDiagram;
import org.openlca.app.analysis.sankey.SankeySelectionDialog;
import org.openlca.app.resources.ImageType;

/**
 * Action for opening the {@link SankeySelectionDialog} and updating the sankey
 * diagram
 * 
 * @author Sebastian Greve
 * 
 */
public class PropertySelectionAction extends Action {

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
		SankeySelectionDialog dialog = new SankeySelectionDialog(
				sankeyDiagram.getFlows(), sankeyDiagram.getLCIACategories(),
				sankeyDiagram.getDatabase());
		dialog.setCutoff(cutoff);
		dialog.setSelection(lastSelection);
		if (dialog.open() == Window.OK) {
			lastSelection = dialog.getSelection();
			cutoff = dialog.getCutoff();
			sankeyDiagram.update(lastSelection, cutoff);
		}
	}

}
