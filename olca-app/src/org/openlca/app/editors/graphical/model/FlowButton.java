package org.openlca.app.editors.graphical.model;

import java.awt.Rectangle;

import org.eclipse.draw2d.AbstractBackground;
import org.eclipse.draw2d.ActionEvent;
import org.eclipse.draw2d.ActionListener;
import org.eclipse.draw2d.Clickable;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.openlca.app.editors.graphical.action.ExchangeAddAction;
import org.openlca.app.util.Colors;

public class FlowButton extends Clickable {

	public FlowButton(ExchangeFigure ef, boolean forInputs, ProcessNode node) {
		super(new FlowLabel(" + add flow "));
		var layout = new GridLayout(1, true);
		layout.marginHeight = 3;
		layout.marginWidth = 5;
		setLayoutManager(layout);
		layout.setConstraint(this, new Rectangle(0, 0, -1, -1));
		setBackgroundColor(Colors.get(135, 76, 63));
		setOpaque(false);
		addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (forInputs) {
					ExchangeAddAction.forInput().runAddFlow(node);
				} else
					ExchangeAddAction.forOutput().runAddFlow(node);
			}
		});
	}

	private static class FlowLabel extends Label {
		
		private FlowLabel(String text) {
			super(text);
		}
		
		@Override
		protected void paintFigure(Graphics graphics) {
			graphics.fillRoundRectangle(getBounds(), 5, 5);
			if (getBorder() instanceof AbstractBackground)
				((AbstractBackground) getBorder()).paintBackground(this, graphics,
						NO_INSETS);
			super.paintFigure(graphics);
		}
		
	}
}