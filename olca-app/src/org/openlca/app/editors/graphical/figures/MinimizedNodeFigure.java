package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.GridLayout;
import org.openlca.app.components.graphics.figures.GridPos;
import org.openlca.app.components.graphics.figures.RoundBorder;
import org.openlca.app.editors.graphical.model.Node;

public class MinimizedNodeFigure extends NodeFigure {

	public MinimizedNodeFigure(Node node) {
		super(node);
		var theme = node.getGraph().getEditor().getTheme();
		var box = node.getThemeBox();

		var layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		setLayoutManager(layout);

		var header = new NodeHeader();
		var roundedCorners = RoundBorder.Corners
			.fullRoundedCorners(HEADER_ARC_SIZE);
		var headerBorder = new RoundBorder(theme.boxBorderWidth(box), roundedCorners);
		headerBorder.setColor(borderColor());
		header.setBorder(headerBorder);
		add(header, GridPos.fill());
		onAnalysisGroupChange(() -> {
			header.getLabel().setText(name());
			headerBorder.setColor(borderColor());
			header.setBorder(headerBorder);
		});

		setOpaque(true);
	}

}
