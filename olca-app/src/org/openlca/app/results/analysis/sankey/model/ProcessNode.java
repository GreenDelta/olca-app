package org.openlca.app.results.analysis.sankey.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.results.Sankey;

public class ProcessNode {

	public static String CONNECTION = "Connection";

	public final TechFlow product;
	public final ProductSystemNode parent;

	public ProcessFigure figure;
	public final List<Link> links = new ArrayList<>();

	public final double totalResul;
	public final double totalShare;
	public final double directResult;
	public final double directShare;

	ProcessPart editPart;
	private Rectangle layoutConstraints = new Rectangle(0, 0, 0, 0);

	public ProcessNode(ProductSystemNode parent, Sankey.Node node) {
		this.parent = parent;
		this.product = node.product;
		this.totalResul = node.total;
		this.totalShare = node.share;
		this.directResult = node.direct;

		// calculate the direct result share
		var sankey = parent.editor.sankey;
		if (sankey == null || sankey.root == null) {
			directShare = 0;
		} else {
			var total = sankey.root.total;
			directShare = total != 0
					? node.direct / total
					: 0;
		}
	}

	public Rectangle getLayoutConstraints() {
		return layoutConstraints;
	}

	public void setLayoutConstraints(Rectangle constraints) {
		this.layoutConstraints = constraints;
	}

}
