package org.openlca.app.editors.graphical.view;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.swt.SWT;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.core.model.descriptors.Descriptor;

public class ExchangeFigure extends Figure {

	final ExchangeNode node;
	private final Label label;

	public ExchangeFigure(ExchangeNode node) {
		this.node = node;
		var layout = new GridLayout(1, true);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		setLayoutManager(layout);
		label = new Label(node.getName());
		if (node.isRefFlow()) {
			label.setFont(UI.boldFont());
		}
		setToolTip(new Label(tooltip()));
		add(label, new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
	}

	private String tooltip() {
		var exchange = node.exchange;
		if (exchange == null || exchange.flow == null)
			return "";
		var type = node.flowType();
		var prefix = type == null
			? "?"
			: Labels.of(type);
		if (exchange.isAvoided) {
			prefix += " - avoided";
		}
		var text = prefix + ": " + node.getName() + "\n";
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

	@Override
	public void paint(Graphics g) {
		var theme = node.config().theme();
		label.setForegroundColor(theme.labelColor(node.flowType()));
		super.paint(g);
	}
}
