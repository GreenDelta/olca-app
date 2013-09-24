package org.openlca.app.analysis.sankey;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.resources.ImageType;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.AnalysisResult;

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
		AnalysisResult result = sankeyDiagram.getResult();
		if (result == null)
			return;
		EntityCache cache = Cache.getEntityCache();
		Set<FlowDescriptor> flows = result.getFlowResults().getFlows(cache);
		Set<ImpactCategoryDescriptor> impacts = null;
		if (result.hasImpactResults())
			impacts = result.getImpactResults().getImpacts(cache);
		else
			impacts = Collections.emptySet();
		openAndUpdate(flows, impacts);
	}

	private void openAndUpdate(Set<FlowDescriptor> flows,
			Set<ImpactCategoryDescriptor> impacts) {
		SankeySelectionDialog dialog = new SankeySelectionDialog(flows, impacts);
		dialog.setCutoff(cutoff);
		dialog.setSelection(lastSelection);
		if (dialog.open() == Window.OK) {
			lastSelection = dialog.getSelection();
			cutoff = dialog.getCutoff();
			sankeyDiagram.update(lastSelection, cutoff);
		}
	}
}
