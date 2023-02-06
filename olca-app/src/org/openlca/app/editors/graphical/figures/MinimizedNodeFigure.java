package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.*;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.tools.graphics.themes.Theme;
import org.openlca.app.tools.graphics.figures.GridPos;
import org.openlca.app.tools.graphics.figures.RoundBorder;

public class MinimizedNodeFigure extends NodeFigure {

	public MinimizedNodeFigure(Node node) {
		super(node);
		var theme = node.getGraph().getConfig().getTheme();
		var box = Theme.Box.of(node.descriptor, node.isOfReferenceProcess());

		var layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		setLayoutManager(layout);

		header = new NodeHeader();
		var roundedCorners = RoundBorder.Corners
			.fullRoundedCorners(HEADER_ARC_SIZE);
		var headerBorder = new RoundBorder(theme.boxBorderWidth(box), roundedCorners);
		headerBorder.setColor(theme.boxBorderColor(box));
		header.setBorder(headerBorder);
		add(header, GridPos.fill());

		setOpaque(true);
	}

}
