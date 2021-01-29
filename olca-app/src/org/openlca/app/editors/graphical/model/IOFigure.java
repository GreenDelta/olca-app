package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.swt.SWT;

class IOFigure extends Figure {

	private final ProcessNode node;
	private final ExchangePanel inputPanel;
	private final ExchangePanel outputPanel;

	IOFigure(ProcessNode node) {
		this.node = node;
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

	private class Header extends Figure {

		Header(String text) {
			var layout = new GridLayout(1, true);
			layout.marginHeight = 2;
			layout.marginWidth =  5;
			setLayoutManager(layout);
			var label = new Label(text);
			var theme = node.config().theme();
			label.setForegroundColor(
				theme.ioHeaderForegroundOf(node));
			add(label, new GridData(SWT.CENTER, SWT.TOP, true, false));
		}
	}

	private class ExchangePanel extends Figure {

		ExchangePanel() {
			var layout = new GridLayout(1, true);
			layout.marginHeight = 2;
			layout.marginWidth =  5;
			setLayoutManager(layout);
			var theme = node.config().theme();
			setBorder(new LineBorder(theme.boxBorderOf(node), 1));
		}

		@Override
		protected void paintFigure(Graphics g) {
			var theme = node.config().theme();
			g.pushState();
			g.setBackgroundColor(
				theme.ioInnerBackgroundOf(node));
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
