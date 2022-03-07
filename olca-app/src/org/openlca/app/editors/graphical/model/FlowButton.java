package org.openlca.app.editors.graphical.model;

import java.awt.Rectangle;

import org.eclipse.draw2d.ActionEvent;
import org.eclipse.draw2d.ActionListener;
import org.eclipse.draw2d.Button;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.action.ExchangeAddAction;
import org.openlca.app.util.Colors;

public class FlowButton extends Figure {
	Button button;

	public FlowButton(ExchangeFigure ef, boolean forInputs, ProcessNode node) {

		var layout = new GridLayout(1, true);
		layout.marginHeight = 3;
		layout.marginWidth = 5;
		setLayoutManager(layout);
		layout.setConstraint(button, new Rectangle(0, 0, -1, -1));
		button = new Button("+ add flow");
		button.setPreferredSize(100, 20);
		button.setBackgroundColor(Colors.get(135, 76, 63));
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (forInputs) {
					ExchangeAddAction.forInput().runAddFlow(node);
				} else
					ExchangeAddAction.forOutput().runAddFlow(node);
			}
		});
		var alignment = forInputs ? SWT.LEFT : SWT.RIGHT;
		add(button, new GridData(alignment, SWT.TOP, true, false));
	}

	@Override
	public void paint(Graphics g) {
		g.drawRoundRectangle(getBounds(), 15, 15);
		super.paint(g);
	}
}