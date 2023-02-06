package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.graphics.figures.RoundBorder;
import org.openlca.app.tools.graphics.figures.SVGImage;
import org.openlca.app.tools.graphics.themes.Theme;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.tools.graphics.figures.ComponentFigure;
import org.openlca.app.tools.graphics.figures.GridPos;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.Colors;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.descriptors.Descriptor;

public class ExchangeFigure extends ComponentFigure {

	private static final Integer SIGNIF_NUMBER = 2;
	public final static Dimension BORDER_ARC_SIZE = new Dimension(6, 6);
	private final Theme theme;
	public ExchangeItem exchangeItem;
	private final Exchange exchange;
	private Label label;
	private Figure amount;
	private Label unitLabel;
	private boolean selected;
	private IOPaneFigure paneFigure;

	public ExchangeFigure(ExchangeItem exchangeItem) {
		super(exchangeItem);
		this.exchangeItem = exchangeItem;
		this.exchange = exchangeItem.exchange;
		this.theme = exchangeItem.getGraph().getConfig().getTheme();

		var layout = new GridLayout(4, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		setLayoutManager(layout);

		var corners = RoundBorder.Corners.fullRoundedCorners(BORDER_ARC_SIZE);
		var border = new RoundBorder(1, corners);
		setBorder(border);

		addMouseMotionListener(new BorderMouseListener());

		setToolTip(new Label(getExchangeTooltip()));
	}

	public void setChildren(IOPaneFigure paneFigure) {
		this.paneFigure = paneFigure;

		label = new Label(Labels.name(exchange.flow));
		label.setForegroundColor(theme.labelColor(exchangeItem.flowType()));

		var renderedImage = Images.getSVG(exchange.flow);
		if (renderedImage != null) {
			var scalableImage = new SVGImage(renderedImage, true, true, true);
			var height = label.getPreferredSize().height();
			add(scalableImage, GridPos.leadCenter());
			var size = height - 2 * ((RoundBorder) getBorder()).getWidth();
			scalableImage.setPreferredImageSize(size, size);
		}

		add(label, new GridData(SWT.LEAD, SWT.CENTER, true, false));

		var flowColor = theme.labelColor(exchangeItem.flowType());
		amount = exchange.formula != null
				? getAmountWithFormula(exchange, flowColor)
				: getAmount(exchange, flowColor);
		add(amount);

		unitLabel = new Label(Labels.name(exchange.unit));
		unitLabel.setLabelAlignment(PositionConstants.LEFT);
		unitLabel.setForegroundColor(theme.labelColor(exchangeItem.flowType()));
		add(unitLabel);

		setChildrenConstraints();
	}

	private static Figure getAmount(Exchange exchange, Color color) {
		var amount = new Label(Numbers.format(exchange.amount, SIGNIF_NUMBER));
		if (color != null)
			amount.setForegroundColor(color);
		amount.setLabelAlignment(PositionConstants.RIGHT);
		return amount;
	}

	private static Figure getAmountWithFormula(Exchange exchange, Color color) {
		var amount = new Figure();
		var layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 1;
		amount.setLayoutManager(layout);

		var image = new ImageFigure(Icon.FORMULA.get());
		image.setToolTip(new Label(exchange.formula));
		amount.add(image);

		var label = new Label(Numbers.format(exchange.amount, SIGNIF_NUMBER));
		if (color != null)
			label.setForegroundColor(color);
		label.setLabelAlignment(PositionConstants.RIGHT);
		amount.add(label);

		return amount;
	}

	private String getExchangeTooltip() {
		if (exchange == null || exchange.flow == null)
			return "";
		var type = exchangeItem.flowType();
		var prefix = type == null
				? "?"
				: Labels.of(type);
		if (exchange.isAvoided) {
			prefix += " - avoided";
		}
		var text = prefix + ": " + Labels.name(exchange.flow) + "\n";
		if (exchange.flow.category != null) {
			text += M.Category + ": " + Labels.category(
					Descriptor.of(exchange.flow)) + "\n";
		}
		text += M.Amount + ": "
				+ Numbers.format(exchange.amount)
				+ " " + Labels.name(exchange.unit);

		if (exchange.formula != null)
			text += "\n" + M.Formula + ": " + exchange.formula;

		return text;
	}

	@Override
	protected void paintFigure(Graphics g) {
		g.pushState();

		if (selected) {
			g.setBackgroundColor(Colors.gray());
			g.fillRectangle(getBounds());
		}
		if (exchangeItem.isQuantitativeReference())
			setBold(true);

		g.setForegroundColor(ColorConstants.white);
		g.popState();

		super.paintFigure(g);
	}

	public void setBold(boolean b) {
		if (b) label.setFont(UI.boldFont());
		else label.setFont(null);
	}

	/**
	 * Sets the selection state of this ExchangeFigure
	 *
	 * @param b true will cause the figure to appear selected
	 */
	public void setSelected(boolean b) {
		selected = b;
		repaint();
	}

	public static Dimension getPreferredAmountLabelSize(ExchangeItem item) {
		var amount = item.exchange.formula != null
				? getAmountWithFormula(item.exchange, null)
				: getAmount(item.exchange, null);
		return amount.getPreferredSize(SWT.DEFAULT, SWT.DEFAULT);
	}

	public static Dimension getPreferredUnitLabelSize(ExchangeItem item) {
		var unit = new Label(Labels.name(item.exchange.unit));
		return unit.getPreferredSize(SWT.DEFAULT, SWT.DEFAULT);
	}

	public IOPaneFigure getIOPaneFigure() {
		// The parent of an ExchangeFigure is IOPaneFigure.contentPane.
		return (IOPaneFigure) super.getParent().getParent();
	}

	public NodeFigure getNodeFigure() {
		// The parent of an ExchangeFigure is IOPaneFigure.contentPane.
		return getIOPaneFigure().getNodeFigure();
	}

	public void setChildrenConstraints() {
		var amountPrefSize = paneFigure.getAmountLabelSize();
		setConstraint(amount,
				new GridData(amountPrefSize.width, amountPrefSize.height));
		var unitPrefSize = paneFigure.getUnitLabelSize();
		setConstraint(unitLabel,
				new GridData(unitPrefSize.width, unitPrefSize.height));
	}

	public void setHighlighted(boolean b) {
		var border = (LineBorder) getBorder();
		if (b) {
			var node = exchangeItem.getNode();
			var box = Theme.Box.of(node.descriptor, node.isOfReferenceProcess());
			var color = theme.boxBorderColor(box);
			border.setColor(color);
			border.setStyle(SWT.LINE_DASH);
		} else {
			var backgroundColor = theme.backgroundColor();
			border.setColor(backgroundColor);
			border.setStyle(SWT.LINE_SOLID);
			repaint();
		}
	}

	public String toString() {
		var name = Labels.name(exchange.flow);
		return "ExchangeFigure("
				+ name.substring(0, Math.min(name.length(), 20)) + ")";
	}

	private class BorderMouseListener extends MouseMotionListener.Stub {

		@Override
		public void mouseEntered(MouseEvent me) {
			var figure = (IFigure) me.getSource();
			((LineBorder) figure.getBorder()).setColor(Colors.gray());
			figure.repaint();
		}

		@Override
		public void mouseExited(MouseEvent me) {
			final IFigure figure = (IFigure) me.getSource();
			var backgroundColor = theme.backgroundColor();
			((LineBorder) figure.getBorder()).setColor(backgroundColor);
			figure.repaint();
		}

	}

}
