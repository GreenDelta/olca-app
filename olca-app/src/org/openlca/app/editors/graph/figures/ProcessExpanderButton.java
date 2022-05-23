package org.openlca.app.editors.graph.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.editors.graph.themes.Theme;

public class ProcessExpanderButton extends Button {

	public ProcessExpanderButton() {
		setBorder(new ButtonBorder(ButtonBorder.SCHEMES.TOOLBAR));
	}

	@Override
	public void setEnabled(boolean value) {
		super.setEnabled(value);
		var label = new Label("+");
		if (value)
			label.setForegroundColor(ColorConstants.black);
		else
			label.setForegroundColor(ColorConstants.gray);
		setContents(label);

	}

	static class PlusFigure extends Figure {

		private final Node node;

		PlusFigure(Node node) {
			this.node = node;
		}

		@Override
		public void paintFigure(Graphics g) {
			var theme = node.getConfig().getTheme();
			var box = Theme.Box.of(node);
			var location = getLocation();
			g.setForegroundColor(theme.boxBorderColor(box));
			var r = new Rectangle(location.x, location.y, 25, 25);
			g.drawOval(r);
			g.setForegroundColor(ColorConstants.red);
			g.restoreState();
			super.paintFigure(g);
		}
	}

}
