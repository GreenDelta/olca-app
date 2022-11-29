package org.openlca.app.results.analysis.sankey.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.M;
import org.openlca.app.tools.graphics.figures.GridPos;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.analysis.sankey.model.SankeyNode;
import org.openlca.app.tools.graphics.figures.ComponentFigure;
import org.openlca.app.tools.graphics.figures.RoundBorder;
import org.openlca.app.tools.graphics.themes.Theme;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;

public class SankeyNodeFigure extends ComponentFigure {

	private static final Integer PERCENTAGE_SIGNIF_NUMBER = 3;
	private static final Integer SIGNIF_NUMBER = 3;
	public final static Dimension HEADER_ARC_SIZE = new Dimension(15, 15);

	public final SankeyNode node;
	private final Figure contentPane = new Figure();
	private final Theme theme;
	private final Theme.Box box;

	public SankeyNodeFigure(SankeyNode node) {
		super(node);
		this.node = node;
		theme = node.getDiagram().getConfig().getTheme();
		box = Theme.Box.of(node.product.provider(), node.isReference());

		GridLayout layout = new GridLayout(1, false);
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
		border.setColor(theme.boxBorderColor(box));
		setBorder(border);

		var header = new SankeyNodeHeader();
		var topRoundedCorners = RoundBorder.Corners
				.topRoundedCorners(HEADER_ARC_SIZE);
		var headerBorder = new RoundBorder(borderWidth, topRoundedCorners);
		headerBorder.setColor(theme.boxBorderColor(box));
		header.setBorder(headerBorder);
		add(header, new GridData(SWT.FILL, SWT.FILL, true, false));

		var contentPaneLayout = new GridLayout(1, false);
		contentPaneLayout.marginHeight = 2;
		contentPaneLayout.marginWidth = 4;
		contentPaneLayout.horizontalSpacing = 0;
		contentPaneLayout.verticalSpacing = 0;
		contentPane.setLayoutManager(contentPaneLayout);
		add(contentPane, GridPos.fill());

		contentPane.add(createDirectLabel(), GridPos.centerCenter());
		contentPane.add(createDirectValue(), GridPos.centerCenter());
		contentPane.add(createUpstreamLabel(), GridPos.centerCenter());
		contentPane.add(createUpstreamValue(), GridPos.centerCenter());

		setBackgroundColor(theme.boxBackgroundColor(box));
		setOpaque(true);
	}

	private Label createDirectLabel() {
		var percentage = Numbers.format(node.directShare * 100,
				PERCENTAGE_SIGNIF_NUMBER);
		var label = new Label(M.Direct + " (" + percentage + "%)" + ":");
		label.setForegroundColor(theme.boxFontColor(box));
		label.setFont(UI.boldFont());
		return label;
	}

	private Label createDirectValue() {
		var val = Numbers.format(node.node.direct, SIGNIF_NUMBER);
		var label = new Label(val + " " + node.unit);
		label.setForegroundColor(theme.boxFontColor(box));
		return label;
	}

	private Label createUpstreamLabel() {
		var percentage = Numbers.format(node.node.share * 100,
				PERCENTAGE_SIGNIF_NUMBER);
		var label = new Label(M.UpstreamTotal + " (" + percentage + "%)" + ":");
		label.setForegroundColor(theme.boxFontColor(box));
		label.setFont(UI.boldFont());
		return label;
	}

	private Label createUpstreamValue() {
		var val = Numbers.format(node.node.total, SIGNIF_NUMBER);
		var label = new Label(val + " " + node.unit);
		label.setForegroundColor(theme.boxFontColor(box));
		return label;
	}

	class SankeyNodeHeader extends Figure {

		SankeyNodeHeader() {
			var theme = node.getDiagram().getConfig().getTheme();
			var box = Theme.Box.of(node.product.provider(), node.isReference());

			GridLayout layout = new GridLayout(2, false);
			layout.marginHeight = 2;
			layout.marginWidth = 3;
			setLayoutManager(layout);

			add(new ImageFigure(Images.get(node.product.provider())),
					new GridData(SWT.LEAD, SWT.CENTER, false, true));

			var label = new Label(Labels.name(node.product));
			label.setForegroundColor(theme.boxFontColor(box));
			add(label, new GridData(SWT.LEAD, SWT.CENTER, true, true));

			setBackgroundColor(theme.boxBackgroundColor(box));
			setOpaque(true);
		}

	}

}
