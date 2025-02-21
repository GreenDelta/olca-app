package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.M;
import org.openlca.app.components.graphics.figures.ComponentFigure;
import org.openlca.app.components.graphics.figures.GridPos;
import org.openlca.app.components.graphics.figures.RoundBorder;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.descriptors.Descriptor;

public class ExchangeFigure extends ComponentFigure {

	private static final Integer SIGNIF_NUMBER = 2;
	public final static Dimension BORDER_ARC_SIZE = new Dimension(6, 6);
	private final Theme theme;
	public ExchangeItem item;
	private final Exchange exchange;

	private ImageFigure icon;
	private Label label;
	private Figure amount;
	private Label unitLabel;

	private boolean selected;
	private IOPaneFigure paneFigure;

	public ExchangeFigure(ExchangeItem item) {
		super(item);
		this.item = item;
		this.exchange = item.exchange;
		this.theme = item.getGraph().getEditor().getTheme();

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

		icon = new ImageFigure(getIconImage());
		add(icon, GridPos.leadCenter());

		label = new Label(Labels.name(exchange.flow));
		label.setForegroundColor(theme.labelColor(item.flowType()));
		if (exchange.isAvoided) {
			setItalic(true);
		}
		add(label, new GridData(SWT.LEAD, SWT.CENTER, true, false));

		var flowColor = theme.labelColor(item.flowType());
		amount = exchange.formula != null
				? getAmountWithFormula(exchange, flowColor)
				: getAmount(exchange, flowColor);
		add(amount);

		unitLabel = new Label(Labels.name(exchange.unit));
		unitLabel.setLabelAlignment(PositionConstants.LEFT);
		unitLabel.setForegroundColor(theme.labelColor(item.flowType()));
		add(unitLabel);

		setChildrenConstraints();
	}

	private Image getIconImage() {
		if (item.isInput()
				&& item.isProduct()
				&& !item.isConnected())
			return Icon.FLOW_PRODUCT_UNLINKED.get();
		if (item.isOutput()
				&& item.isWaste()
				&& !item.isConnected())
			return Icon.FLOW_WASTE_UNLINKED.get();
		return Images.get(exchange.flow);
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
		var type = item.flowType();
		var prefix = type == null
				? "?"
				: Labels.of(type);
		if (exchange.isAvoided) {
			prefix += " - " + M.Avoided;
		}
		var text = prefix + " - " + Labels.name(exchange.flow) + "\n";
		if (exchange.flow.category != null) {
			text += M.Category + " - " + Labels.category(
					Descriptor.of(exchange.flow)) + "\n";
		}
		text += M.Amount + " - "
				+ Numbers.format(exchange.amount)
				+ " " + Labels.name(exchange.unit);

		if (exchange.formula != null)
			text += "\n" + M.Formula + " - " + exchange.formula;

		return text;
	}

	@Override
	protected void paintFigure(Graphics g) {
		g.pushState();

		if (selected) {
			g.setBackgroundColor(Colors.gray());
			g.fillRectangle(getBounds());
		}
		if (item.isQuantitativeReference()) {
			setBold(true);
		}
		if (icon != null) {
			icon.setImage(getIconImage());
		}

		g.setForegroundColor(ColorConstants.white);
		g.popState();

		super.paintFigure(g);
	}

	public void setBold(boolean b) {
		if (b) label.setFont(UI.boldFont());
		else label.setFont(null);
	}

	public void setItalic(boolean b) {
		if (b) label.setFont(UI.italicFont());
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
			var node = item.getNode();
			var box = node.getThemeBox();
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
