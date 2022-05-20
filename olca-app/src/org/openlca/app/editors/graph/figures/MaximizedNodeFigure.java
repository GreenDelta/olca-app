package org.openlca.app.editors.graph.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.editors.graph.themes.Theme;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.core.model.descriptors.RootDescriptor;

public class MaximizedNodeFigure extends NodeFigure {

	private final Figure contentPane = new Figure();

	public MaximizedNodeFigure(Node node) {
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

		var roundedCorners = RoundBorder.Corners
			.fullRoundedCorners(new Dimension(ARC_SIZE, ARC_SIZE));
		var border = new RoundBorder(theme.boxBorderWidth(box), roundedCorners);
		border.setColor(theme.boxBorderColor(box));
		setBorder(border);

		setToolTip(new Label(name));
		setForegroundColor(theme.boxFontColor(box));
		setBackgroundColor(theme.boxBackgroundColor(box));

		var header = new NodeHeader();
		var topRoundedCorners = RoundBorder.Corners
			.topRoundedCorners(new Dimension(ARC_SIZE, ARC_SIZE));
		var headerBorder = new RoundBorder(theme.boxBorderWidth(box),
			topRoundedCorners);
		headerBorder.setColor(theme.boxBorderColor(box));
		header.setBorder(headerBorder);
		add(header, new GridData(SWT.FILL, SWT.FILL, true, false));

		var contentPaneLayout = new GridLayout(1, false);
		contentPaneLayout.marginHeight = 0;
		contentPaneLayout.marginWidth = 0;
		contentPaneLayout.horizontalSpacing = 0;
		contentPaneLayout.verticalSpacing = 0;
		contentPane.setLayoutManager(contentPaneLayout);
		add(contentPane, GridPos.fill());

		setOpaque(true);
	}

	public IFigure getContentPane() {
		return contentPane;
	}

}
