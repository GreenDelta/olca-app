package org.openlca.app.results.analysis.sankey.layout;

import java.util.List;

import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.results.analysis.sankey.model.ProcessEditPart;
import org.openlca.app.results.analysis.sankey.model.ProcessNode;
import org.openlca.app.results.analysis.sankey.model.ProductSystemEditPart;
import org.openlca.app.results.analysis.sankey.model.ProductSystemNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphLayoutManager extends AbstractLayout {

	public static int horizontalSpacing = 100;
	public static int verticalSpacing = 200;

	private ProductSystemEditPart diagram;
	private Logger log = LoggerFactory.getLogger(getClass());

	public GraphLayoutManager(ProductSystemEditPart diagram) {
		this.diagram = diagram;
	}

	@Override
	public void layout(IFigure container) {
		log.trace("Layout product system figure");
		if (diagram == null)
			return;
		for (Object aPart : diagram.getChildren()) {
			if (aPart instanceof ProcessEditPart) {
				ProcessEditPart part = (ProcessEditPart) aPart;
				ProcessNode node = (ProcessNode) part.getModel();
				part.getFigure().setBounds(node.getXyLayoutConstraints());
			}
		}
	}

	@Override
	protected Dimension calculatePreferredSize(IFigure container, int hint,
			int hint2) {
		container.validate();
		List<?> children = container.getChildren();
		Rectangle result = new Rectangle().setLocation(container
				.getClientArea().getLocation());
		for (int i = 0; i < children.size(); i++) {
			result.union(((IFigure) children.get(i)).getBounds());
		}
		result.resize(container.getInsets().getWidth(), container.getInsets()
				.getHeight());
		return result.getSize();
	}

	public void layoutTree() {
		log.trace("Apply tree-layout");
		if (diagram != null && diagram.getModel() != null) {
			TreeLayout layout = new TreeLayout();
			layout.layout((ProductSystemNode) diagram.getModel());
		}
	}

}
