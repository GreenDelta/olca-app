package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.*;
import org.eclipse.swt.SWT;
import org.openlca.app.util.Colors;

public class AddExchangeButton extends Clickable {

	private static final Integer ARC_SIZE = 5;

	public AddExchangeButton() {
		super(new AddExchangeButtonFigure("+ add flow"));
		var layout = new GridLayout(1, true);
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		setLayoutManager(layout);
	}

	private static class AddExchangeButtonFigure extends Figure {

		private AddExchangeButtonFigure(String text) {
			var layout = new GridLayout(1, true);
			layout.marginHeight = 2;
			layout.marginWidth = 6;
			setLayoutManager(layout);
			setBackgroundColor(Colors.get(135, 76, 63));
			add(new Label(text), new GridData(SWT.CENTER, SWT.CENTER, false, false));
			setOpaque(false);
		}

		@Override
		protected void paintFigure(Graphics g) {
			g.setAntialias(SWT.ON);
			g.fillRoundRectangle(getBounds(), ARC_SIZE, ARC_SIZE);
			if (getBorder() instanceof AbstractBackground background) {
				background.paintBackground(this, g, NO_INSETS);
			}
			super.paintFigure(g);
		}
	}
}
