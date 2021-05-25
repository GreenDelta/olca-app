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

		// layout
		var layout = new GridLayout(1, true);
		layout.horizontalSpacing = 4;
		layout.verticalSpacing = 4;
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		setLayoutManager(layout);

		add(new Header(true),
			new GridData(SWT.FILL, SWT.TOP, true, false));
		inputPanel = new ExchangePanel();
		add(inputPanel,
			new GridData(SWT.FILL, SWT.FILL, true, true));

		add(new Header(false),
			new GridData(SWT.FILL, SWT.TOP, true, false));
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

		// delegate exchange figures to the respective input or
		// output panel
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

		private final Label label;

		Header(boolean forInputs) {
			var layout = new GridLayout(1, true);
			layout.marginHeight = 2;
			layout.marginWidth =  5;
			setLayoutManager(layout);
			label = new Label(forInputs
				? ">> Input flows"
				: "Output flows >>");
			var alignment = forInputs
				? SWT.LEFT
				: SWT.RIGHT;
			add(label, new GridData(alignment, SWT.TOP, true, false));
		}

		@Override
		public void paint(Graphics g) {
			var theme = node.config().theme();
			label.setForegroundColor(
				theme.ioHeaderForegroundOf(node));
			super.paint(g);
		}
	}

	private class ExchangePanel extends Figure {

		private final LineBorder border;

		ExchangePanel() {
			var layout = new GridLayout(1, true);
			layout.marginHeight = 2;
			layout.marginWidth =  5;
			setLayoutManager(layout);
			border = new LineBorder(1);
			setBorder(border);
		}

		@Override
		protected void paintFigure(Graphics g) {
			var theme = node.config().theme();
			border.setColor(theme.boxBorderOf(node));
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
