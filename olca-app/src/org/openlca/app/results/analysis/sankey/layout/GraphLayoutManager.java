package org.openlca.app.results.analysis.sankey.layout;

import java.util.List;

import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.results.analysis.sankey.model.ProcessNode;
import org.openlca.app.results.analysis.sankey.model.ProcessPart;
import org.openlca.app.results.analysis.sankey.model.ProductSystemNode;
import org.openlca.app.results.analysis.sankey.model.ProductSystemPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphLayoutManager extends AbstractLayout {

	public static int horizontalSpacing = 100;
	public static int verticalSpacing = 200;

	private final ProductSystemPart diagram;
	private final Logger log = LoggerFactory.getLogger(getClass());

	public GraphLayoutManager(ProductSystemPart diagram) {
		this.diagram = diagram;
	}

	@Override
	public void layout(IFigure container) {
		log.trace("Layout product system figure");
		if (diagram == null)
			return;
		for (Object aPart : diagram.getChildren()) {
			if (aPart instanceof ProcessPart) {
				ProcessPart part = (ProcessPart) aPart;
				ProcessNode node = part.getModel();
				part.getFigure().setBounds(node.getLayoutConstraints());
			}
		}
	}

	@Override
	protected Dimension calculatePreferredSize(IFigure container, int hint,
			int hint2) {
		container.validate();
		List<?> children = container.getChildren();
		Rectangle result = new Rectangle().setLocation(
				container.getClientArea().getLocation());
		for (Object child : children) {
			result.union(((IFigure) child).getBounds());
		}
		result.resize(container.getInsets().getWidth(), container.getInsets().getHeight());
		return result.getSize();
	}

	public void layoutTree() {
		log.trace("Apply tree-layout");
		if (diagram != null && diagram.getModel() != null) {
			// TreeLayout layout = new TreeLayout();
			// layout.layout((ProductSystemNode) diagram.getModel());
			new BlockLevelLayout().applyOn((ProductSystemNode) diagram.getModel());
		}
	}

}
