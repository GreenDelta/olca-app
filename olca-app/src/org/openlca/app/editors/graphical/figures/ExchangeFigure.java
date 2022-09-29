package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.themes.Theme;
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
	private final Theme theme;
	public ExchangeItem exchangeItem;
	private final Exchange exchange;
	private Label label;
	private Label amountLabel;
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

		var border = new LineBorder(1);
		setBorder(border);

		addMouseMotionListener(new MouseMotionListener.Stub() {

			@Override
			public void mouseEntered(MouseEvent me) {
				var figure = (IFigure) me.getSource();
				((LineBorder) figure.getBorder()).setColor(Colors.gray());
				figure.repaint();
			}

			@Override
			public void mouseExited(MouseEvent me) {
				final IFigure figure = (IFigure) me.getSource();
				var backgroundColor = theme.graphBackgroundColor();
				((LineBorder) figure.getBorder()).setColor(backgroundColor);
				figure.repaint();
			}

		});

		setToolTip(new Label(tooltip()));
	}

	public void setChildren(IOPaneFigure paneFigure) {
		this.paneFigure = paneFigure;

		var image = new ImageFigure(Images.get(exchange.flow));
		add(image, GridPos.leadCenter());

		label = new Label(Labels.name(exchange.flow));
		label.setForegroundColor(theme.labelColor(exchangeItem.flowType()));
		add(label, new GridData(SWT.LEAD, SWT.CENTER, true, false));

		amountLabel = exchange.formula != null
				? new Label(exchange.formula)
				: new Label(Numbers.format(exchange.amount, SIGNIF_NUMBER));

		amountLabel.setForegroundColor(theme.labelColor(exchangeItem.flowType()));
		amountLabel.setLabelAlignment(PositionConstants.RIGHT);
		add(amountLabel);

		unitLabel = new Label(Labels.name(exchange.unit));
		unitLabel.setLabelAlignment(PositionConstants.LEFT);
		unitLabel.setForegroundColor(theme.labelColor(exchangeItem.flowType()));
		add(unitLabel);

		setChildrenConstraints();
	}

	private String tooltip() {
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
			text += M.Category + ": " + Labels.getShortCategory(
				Descriptor.of(exchange.flow)) + "\n";
		}
		text += M.Amount + ": "
			+ Numbers.format(exchange.amount)
			+ " " + Labels.name(exchange.unit);
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
			setHighlighted(true);

		g.setForegroundColor(ColorConstants.white);
		g.popState();

		super.paintFigure(g);
	}

	public void setHighlighted(boolean b) {
		if (b) label.setFont(UI.boldFont());
		else label.setFont(null);
	}

	/**
	 * Sets the selection state of this ExchangeFigure
	 *
	 * @param b
	 *            true will cause the figure to appear selected
	 */
	public void setSelected(boolean b) {
		selected = b;
		repaint();
	}

	public static Dimension getPreferredAmountLabelSize(ExchangeItem item) {
		var amountText = item.exchange.formula != null
				? item.exchange.formula
				: Numbers.format(item.exchange.amount, SIGNIF_NUMBER);
		var amount = new Label(amountText);
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
		setConstraint(amountLabel,
			new GridData(amountPrefSize.width, amountPrefSize.height));
		var unitPrefSize = paneFigure.getUnitLabelSize();
		setConstraint(unitLabel,
			new GridData(unitPrefSize.width, unitPrefSize.height));
	}

	public String toString() {
		var name = Labels.name(exchange.flow);
		return "ExchangeFigure("
			+ name.substring(0, Math.min(name.length(), 20)) + ")";
	}

}
