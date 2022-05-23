package org.openlca.app.editors.graph.figures;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.*;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graph.actions.ExchangeAddAction;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.util.Colors;

import java.awt.*;

public class AddFlowButton extends Clickable {

	private static final Integer ARC_SIZE = 5;

	public AddFlowButton(boolean forInputs, Node node) {
		super(new FlowLabel(" + add flow "));
		var layout = new GridLayout(1, true);
		layout.marginHeight = 3;
		layout.marginWidth = 5;
		setLayoutManager(layout);
		layout.setConstraint(this, new Rectangle(0, 0, -1, -1));
		setBackgroundColor(Colors.get(135, 76, 63));
		setOpaque(false);
		addActionListener($ -> {
			var action = forInputs
				? ExchangeAddAction.forInput()
				: ExchangeAddAction.forOutput();
			action.runOn(node);
		});
	}

	private static class FlowLabel extends Label {

		private FlowLabel(String text) {
			super(text);
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
