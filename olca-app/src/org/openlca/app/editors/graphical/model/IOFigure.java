package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.swt.SWT;
import org.openlca.app.util.Colors;

class IOFigure extends Figure {

	private final ExchangePanel inputPanel;
	private final ExchangePanel outputPanel;

	IOFigure() {
		var layout = new GridLayout(2, true);
		layout.horizontalSpacing = 4;
		layout.verticalSpacing = 4;
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		setLayoutManager(layout);
		add(new Header("Input flows"),
			new GridData(SWT.FILL, SWT.TOP, true, false));
		add(new Header("Output flows"),
			new GridData(SWT.FILL, SWT.TOP, true, false));
		inputPanel = new ExchangePanel();
		add(inputPanel,
			new GridData(SWT.FILL, SWT.FILL, true, true));
		outputPanel = new ExchangePanel();
		add(outputPanel,
			new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	@Override
	public void add(IFigure figure, Object constraint, int index) {
		if (!(figure instanceof ExchangeFigure)) {
			super.add(figure, constraint, index);
			return;
		}
		var ef = (ExchangeFigure) figure;
		if (ef.node == null || ef.node.exchange == null)
			return;
		var layout = new GridData(SWT.FILL, SWT.TOP, true, false);
		if (ef.node.exchange.isInput) {
			inputPanel.add(ef, layout);
		} else {
			outputPanel.add(ef, layout);
		}
	}

	private static class Header extends Figure {

		Header(String text) {
			var layout = new GridLayout(1, true);
			layout.marginHeight = 2;
			layout.marginWidth =  5;
			setLayoutManager(layout);
			var label = new Label(text);
			label.setForegroundColor(Colors.white());
			add(label, new GridData(SWT.CENTER, SWT.TOP, true, false));
		}

	}

	private static class ExchangePanel extends Figure {

		ExchangePanel() {
			var layout = new GridLayout(1, true);
			layout.marginHeight = 2;
			layout.marginWidth =  5;
			setLayoutManager(layout);
		}

		@Override
		protected void paintFigure(Graphics g) {
			g.pushState();
			g.setBackgroundColor(Figures.COLOR_LIGHT_GREY);
			var location = getLocation();
			var size = getSize();
			g.fillRectangle(
				location.x,
				location.y,
				size.width,
				size.height);
			g.popState();
			super.paintFigure(g);
		}
	}

}
