package org.openlca.app.editors.graphical.model;

import java.util.Objects;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.swt.SWT;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;

class IOFigure extends Figure {

	private final ProcessNode node;
	private final ExchangePanel inputPanel;
	private final ExchangePanel outputPanel;

	IOFigure(ProcessNode node) {
		this.node = node;
		var layout = new GridLayout(1, true);
		layout.horizontalSpacing = 4;
		layout.verticalSpacing = 4;
		layout.marginHeight = 5;
		layout.marginWidth = 0;
		setLayoutManager(layout);
		inputPanel = initPanel(true);
		outputPanel = initPanel(false);
	}

	private ExchangePanel initPanel(boolean forInputs) {
		add(new Header(forInputs), new GridData(
			SWT.FILL, SWT.TOP, true, false));
		var panel = new ExchangePanel(node);
		add(panel, new GridData(
			SWT.FILL, SWT.FILL, true, true));
		return panel;
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
		var exchange = ef.node.exchange;
		var panel = exchange.isInput
			? inputPanel
			: outputPanel;
		var config = node.config();
		if (config.showFlowIcons) {
			panel.add(
				new ImageFigure(Images.get(ef.node.flowType())),
				new GridData(SWT.LEFT, SWT.TOP, false, false));
		}
		panel.add(ef, new GridData(SWT.FILL, SWT.TOP, true, false));
		if (config.showFlowAmounts) {
			var amount = new Label(Numbers.format(exchange.amount, 2));
			amount.setForegroundColor(config.theme().fontColorOf(node));
			panel.add(amount, new GridData(SWT.RIGHT, SWT.TOP, false, false));
			var unit = new Label(Labels.name(exchange.unit));
			unit.setForegroundColor(config.theme().fontColorOf(node));
			panel.add(unit, new GridData(SWT.LEFT, SWT.TOP, false, false));
		}
	}


	private class Header extends Figure {

		private final Label label;

		Header(boolean forInputs) {
			var layout = new GridLayout(1, true);
			layout.marginHeight = 3;
			layout.marginWidth = 5;
			setLayoutManager(layout);
			label = new Label(forInputs
				? ">> input flows"
				: "output flows >>");
			var alignment = forInputs
				? SWT.LEFT
				: SWT.RIGHT;
			add(label, new GridData(alignment, SWT.TOP, true, false));
		}

		@Override
		public void paint(Graphics g) {
			var theme = node.config().theme();
			var location = getLocation();
			var size = getSize();
			g.setForegroundColor(theme.borderColorOf(node));
			g.drawLine(location.x, location.y, location.x + size.width, location.y);
			g.restoreState();
			label.setForegroundColor(theme.infoFontColor());
			super.paint(g);
		}
	}

	private static class ExchangePanel extends Figure {

		private final ProcessNode node;

		ExchangePanel(ProcessNode node) {
			this.node = node;
			var config = node.config();
			int columns = 1;
			if (config.showFlowAmounts) {
				columns += 1;
			}
			if (config.showFlowIcons) {
				columns += 2;
			}
			var layout = new GridLayout(columns, false);
			layout.marginHeight = 4;
			layout.marginWidth = 5;
			setLayoutManager(layout);
		}

		@Override
		protected void paintFigure(Graphics g) {
			// set a specific background if this is required
			var theme = node.config().theme();
			var background = theme.backgroundColorOf(node);
			if (Objects.equals(background, theme.defaultBackgroundColor())) {
				super.paintFigure(g);
				return;
			}
			g.pushState();
			g.setBackgroundColor(theme.backgroundColorOf(node));
			var loc = getLocation();
			var size = getSize();
			g.fillRectangle(loc.x, loc.y, size.width, size.height);
			g.popState();
			super.paintFigure(g);
		}
	}

}
