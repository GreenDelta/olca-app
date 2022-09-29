package org.openlca.app.results.analysis.sankey.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.M;
import org.openlca.app.tools.graphics.figures.GridPos;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.analysis.sankey.model.SankeyNode;
import org.openlca.app.results.analysis.sankey.themes.Theme;
import org.openlca.app.tools.graphics.figures.ComponentFigure;
import org.openlca.app.tools.graphics.figures.RoundBorder;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;

public class SankeyNodeFigure extends ComponentFigure {

	private static final Integer SIGNIF_NUMBER = 3;
	public final static Dimension HEADER_ARC_SIZE = new Dimension(15, 15);

	public final SankeyNode node;
	private final Figure contentPane = new Figure();

	public SankeyNodeFigure(SankeyNode node) {
		super(node);
		this.node = node;
		var theme = node.getDiagram().getConfig().getTheme();
		var box = Theme.Box.of(node);

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

		var totalVal = Numbers.format(node.totalResult, SIGNIF_NUMBER);
		var totalPerc = Numbers.format(node.totalShare * 100, SIGNIF_NUMBER);

		var contentPaneLayout = new GridLayout(1, false);
		contentPaneLayout.marginHeight = 2;
		contentPaneLayout.marginWidth = 4;
		contentPaneLayout.horizontalSpacing = 0;
		contentPaneLayout.verticalSpacing = 0;
		contentPane.setLayoutManager(contentPaneLayout);
		add(contentPane, GridPos.fill());

		var totalValLabel = new Label(totalVal + node.unit);
		totalValLabel.setForegroundColor(theme.boxFontColor(box));
		totalValLabel.setFont(UI.boldFont());
		var totalPercLabel = new Label(totalPerc + "%");
		totalPercLabel.setForegroundColor(theme.boxFontColor(box));
		contentPane.add(totalValLabel, new GridData(SWT.CENTER, SWT.CENTER, true, true));
		contentPane.add(totalPercLabel, new GridData(SWT.CENTER, SWT.CENTER, true, true));

		setBackgroundColor(theme.boxBackgroundColor(box));
		setToolTip(createToolTip());
		setOpaque(true);
	}

	private Label createToolTip() {
		var singleVal = Numbers.format(node.directResult, SIGNIF_NUMBER);
		var singlePerc = Numbers.format(node.directShare * 100, SIGNIF_NUMBER);
		var totalVal = Numbers.format(node.totalResult, SIGNIF_NUMBER);
		var totalPerc = Numbers.format(node.totalShare * 100, SIGNIF_NUMBER);

		var single = singleVal + " (" + singlePerc + "%)";
		var total = totalVal + " (" + totalPerc + "%)";

		return new Label(
				Labels.name(node.product)
						+ M.Direct + ": " + single + "\n"
						+ M.UpstreamTotal + ": " + total);
	}

	class SankeyNodeHeader extends Figure {

		SankeyNodeHeader() {
			var theme = node.getDiagram().getConfig().getTheme();
			var box = Theme.Box.of(node);

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
