package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.components.graphics.figures.GridPos;
import org.openlca.app.components.graphics.figures.RoundBorder;
import org.openlca.app.editors.graphical.model.Node;

public class MaximizedNodeFigure extends NodeFigure {

	private final Figure contentPane = new Figure();

	public MaximizedNodeFigure(Node node) {
		super(node);
		var theme = node.getGraph().getEditor().getTheme();
		var box = node.getThemeBox();

		var layout = new GridLayout(1, false);
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		setLayoutManager(layout);

		var borderWidth = theme.boxBorderWidth(box);
		// TODO (francois): should be defined in CSS (with probably box.top).
		var topBorderWidth = theme.boxBorderWidth(box);

		var arcDifference = new Dimension(
			2 * layout.marginWidth + (borderWidth + topBorderWidth) / 2,
			2 * layout.marginHeight + (borderWidth + topBorderWidth) / 2);

		var roundedCorners = RoundBorder.Corners
			.fullRoundedCorners(HEADER_ARC_SIZE.getExpanded(arcDifference));
		var border = new RoundBorder(borderWidth, roundedCorners);
		border.setColor(borderColor());
		setBorder(border);

		var header = new NodeHeader();
		var topRoundedCorners = RoundBorder.Corners
			.topRoundedCorners(HEADER_ARC_SIZE);
		var headerBorder = new RoundBorder(borderWidth, topRoundedCorners);
		headerBorder.setColor(borderColor());
		header.setBorder(headerBorder);
		add(header, new GridData(SWT.FILL, SWT.FILL, true, false));
		onAnalysisGroupChange(() -> {
			header.getLabel().setText(name());
			border.setColor(borderColor());
			setBorder(border);
			headerBorder.setColor(borderColor());
			header.setBorder(headerBorder);
		});

		var contentPaneLayout = new GridLayout(1, false);
		contentPaneLayout.marginHeight = 0;
		contentPaneLayout.marginWidth = 4;
		contentPaneLayout.horizontalSpacing = 0;
		contentPaneLayout.verticalSpacing = 0;
		contentPane.setLayoutManager(contentPaneLayout);
		add(contentPane, GridPos.fill());

		setForegroundColor(theme.boxFontColor(box));
		setBackgroundColor(theme.boxBackgroundColor(box));
		setOpaque(true);
	}

	public IFigure getContentPane() {
		return contentPane;
	}
}
