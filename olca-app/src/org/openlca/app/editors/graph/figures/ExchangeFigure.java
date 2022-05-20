package org.openlca.app.editors.graph.figures;

import org.eclipse.draw2d.*;
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

	private ExchangeItem exchangeItem;
	private Exchange exchange;
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
		add(image, new GridData(SWT.LEAD, SWT.CENTER, false, false));

		label = new Label(Labels.name(exchange.flow));
		if (exchangeItem.isRefFlow()) {
			label.setFont(UI.boldFont());
		}
		label.setForegroundColor(theme.labelColor(exchangeItem.flowType()));
		add(label, new GridData(SWT.LEAD, SWT.CENTER, true, false));

		var amount = new Label(Numbers.format(exchange.amount));
		amount.setForegroundColor(ColorConstants.black);
		add(amount, new GridData(SWT.TRAIL, SWT.CENTER, false, false));
		var unit = new Label(Labels.name(exchange.unit));
		unit.setForegroundColor(ColorConstants.black);
		add(unit, new GridData(SWT.LEAD, SWT.CENTER, false, false));

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

	public String toString() {
		var name = Labels.name(exchange.flow);
		return "ExchangeFigure("
			+ name.substring(0, Math.min(name.length(), 20)) + ")";
	}


}
