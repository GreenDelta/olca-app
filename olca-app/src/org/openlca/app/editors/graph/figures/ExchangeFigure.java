package org.openlca.app.editors.graph.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.M;
import org.openlca.app.editors.graph.model.ExchangeItem;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.descriptors.Descriptor;

public class ExchangeFigure extends Figure {

	private final Integer UNIT_ACCURACY = 2;
	public ExchangeItem exchangeItem;
	private final Exchange exchange;
	private final Label label;


	public ExchangeFigure(ExchangeItem exchangeItem) {
		this.exchangeItem = exchangeItem;
		this.exchange = exchangeItem.exchange;

		var theme = exchangeItem.getGraph().getConfig().getTheme();

		var layout = new GridLayout(4, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		setLayoutManager(layout);

		var image = new ImageFigure(Images.get(exchange.flow));
		add(image, GridPos.leadCenter());

		label = new Label(Labels.name(exchange.flow));
		if (exchangeItem.isRefFlow()) {
			label.setFont(UI.boldFont());
		}
		label.setForegroundColor(theme.labelColor(exchangeItem.flowType()));
		add(label, new GridData(SWT.LEAD, SWT.CENTER, true, false));

		var amountLabel = new Label(Numbers.format(exchange.amount, UNIT_ACCURACY));
		amountLabel.setForegroundColor(theme.labelColor(exchangeItem.flowType()));
		amountLabel.setLabelAlignment(PositionConstants.RIGHT);
		var amountPrefSize = getAmountLabelSize();
		add(amountLabel, new GridData(amountPrefSize.width, amountPrefSize.height));

		var unitLabel = new Label(Labels.name(exchange.unit));
		unitLabel.setLabelAlignment(PositionConstants.LEFT);
		var unitPrefSize = getUnitLabelSize();
		unitLabel.setForegroundColor(theme.labelColor(exchangeItem.flowType()));
		add(unitLabel, new GridData(unitPrefSize.width, unitPrefSize.height));

		setToolTip(new Label(tooltip()));
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

	public void setHighlighted(boolean b) {
		if (b) {
			label.setFont(UI.boldFont());
		} else {
			label.setFont(null);
		}
	}

	public Dimension getAmountLabelSize() {
		var size = new Dimension();
		for (ExchangeItem item : exchangeItem.getIOPane().getExchangesItems()) {
			var amountText = Numbers.format(item.exchange.amount, UNIT_ACCURACY);
			var amount = new Label(amountText);
			var preferredSize = amount.getPreferredSize(SWT.DEFAULT, SWT.DEFAULT);
			size.width = Math.max(preferredSize.width, size.width);
			size.height = Math.max(preferredSize.height, size.height);
		}
		return size;
	}

	public Dimension getUnitLabelSize() {
		var size = new Dimension();
		for (ExchangeItem item : exchangeItem.getIOPane().getExchangesItems()) {
			var unit = new Label(Labels.name(item.exchange.unit));
			var preferredSize = unit.getPreferredSize(SWT.DEFAULT, SWT.DEFAULT);
			size.width = Math.max(preferredSize.width, size.width);
			size.height = Math.max(preferredSize.height, size.height);
		}
		return size;
	}

	public IOPaneFigure getIOPaneFigure() {
		// The parent of an ExchangeFigure is IOPaneFigure.contentPane.
		return (IOPaneFigure) super.getParent().getParent();
	}

	public NodeFigure getNodeFigure() {
		// The parent of an ExchangeFigure is IOPaneFigure.contentPane.
		return getIOPaneFigure().getNodeFigure();
	}

	public String toString() {
		var name = Labels.name(exchange.flow);
		return "ExchangeFigure("
			+ name.substring(0, Math.min(name.length(), 20)) + ")";
	}

}
