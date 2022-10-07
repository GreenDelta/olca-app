package org.openlca.app.results.analysis.sankey.model;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.db.Database;
import org.openlca.app.tools.graphics.model.Component;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.Flow;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.LcaResult;
import org.openlca.core.results.Sankey;

import static org.openlca.app.results.analysis.sankey.layouts.SankeyLayout.DEFAULT_LOCATION;

public class SankeyNode extends Component {

	public static final Dimension DEFAULT_SIZE = new Dimension(250, SWT.DEFAULT);

	public final Sankey.Node node;
	public final TechFlow product;
	public final double totalResult;
	public final double totalShare;
	public final double directResult;
	public final double directShare;
	public final String unit;

	public SankeyNode(Sankey.Node node, Sankey<?> sankey, LcaResult result) {
		this.node = node;
		product = node.product;
		totalShare = node.share;
		directResult = node.direct;
		totalResult = node.total;

		var db = Database.get();

		if (sankey.reference instanceof EnviFlow enviFlow) {
			var flow = db.get(Flow.class, enviFlow.flow().id);
			unit = flow.getReferenceUnit().name;
		}
		else if (sankey.reference instanceof ImpactDescriptor impact)
			unit = impact.referenceUnit;
		else if (sankey.reference instanceof CostResultDescriptor cost)
			unit = cost.name;
		else unit = "";

		// calculate the direct result share
		if (sankey == null || sankey.root == null) {
			directShare = 0;
		} else {
			var total = sankey.root.total;
			directShare = total != 0
					? node.direct / total
					: 0;
		}

		setLocation(DEFAULT_LOCATION);
		setSize(DEFAULT_SIZE);
	}

	public Diagram getDiagram() {
		return (Diagram) getParent();
	}

	public boolean isReference() {
		return getDiagram().isReferenceNode(this);
	}

	public String toString() {
		return "Node: "+ Labels.name(node.product.provider());
	}

}
