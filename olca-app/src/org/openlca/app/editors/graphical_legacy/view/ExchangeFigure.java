package org.openlca.app.editors.graphical_legacy.view;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.model.ExchangeNode;
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
		label = new Label(Labels.name(node.exchange.flow));
		if (node.isRefFlow()) {
			label.setFont(UI.boldFont());
		}
		setToolTip(new Label(tooltip()));
		add(label, GridPos.fillTop());
	}

	private String tooltip() {
		var e = node.exchange;
		if (e == null || e.flow == null)
			return "";
		var type = node.flowType();
		var prefix = type == null
			? "?"
			: Labels.of(type);
		if (e.isAvoided) {
			prefix += " - avoided";
		}
		var text = prefix + ": " + Labels.name(e.flow) + "\n";
		if (e.flow.category != null) {
			text += M.Category + ": " + Labels.getShortCategory(
				Descriptor.of(e.flow)) + "\n";
		}
		text += M.Amount + ": "
			+ Numbers.format(e.amount)
			+ " " + Labels.name(e.unit);
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
