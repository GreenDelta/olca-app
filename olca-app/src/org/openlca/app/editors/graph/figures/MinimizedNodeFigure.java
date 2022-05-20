package org.openlca.app.editors.graph.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.editors.graph.themes.Theme;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;

public class MinimizedNodeFigure extends NodeFigure {

	public MinimizedNodeFigure(Node node) {
		super(node);
		var name = Labels.name(node.descriptor);
		var theme = node.getConfig().getTheme();
		var box = Theme.Box.of(node);

		var layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		setLayoutManager(layout);

		setToolTip(new Label(name));
		setForegroundColor(theme.boxFontColor(box));

		var header = new NodeHeader();
		var roundedCorners = RoundBorder.Corners
			.fullRoundedCorners(new Dimension(ARC_SIZE, ARC_SIZE));
		var headerBorder = new RoundBorder(theme.boxBorderWidth(box), roundedCorners);
		headerBorder.setColor(theme.boxBorderColor(box));
		header.setBorder(headerBorder);
		add(header, GridPos.fill());

		setOpaque(true);
	}

}
