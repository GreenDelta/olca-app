package org.openlca.app.editors.graphical.model;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.command.ChangeStateCommand;
import org.openlca.app.editors.graphical.layout.Animation;
import org.openlca.app.editors.graphical.model.ProcessExpander.Side;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.descriptors.ProcessDescriptor;

class ProcessFigure extends Figure {

	static int MINIMUM_HEIGHT = 34;
	static {
		try {
			var boldFont = UI.boldFont();
			if (boldFont != null) {
				var data = boldFont.getFontData();
				int max = 0;
				for (var datum : data) {
					max = Math.max(datum.getHeight(), max);
				}
				MINIMUM_HEIGHT = Math.max(max + 4 + 10, MINIMUM_HEIGHT);
			}
		} catch (Exception ignored) {
		}
	}

	static final int MINIMUM_WIDTH = 250;
	static final int MARGIN_HEIGHT = 2;
	static final int MARGIN_WIDTH = 4;
	private static final int TEXT_HEIGHT = 16;

	private static final Color LINE_COLOR = ColorConstants.gray;
	private static final Color TEXT_COLOR = ColorConstants.black;

	final ProcessNode node;
	private final Color color;

	private ProcessExpander leftExpander;
	private ProcessExpander rightExpander;

	private int minimumHeight = 0;

	ProcessFigure(ProcessNode node) {
		this.node = node;
		this.color = Figures.colorOf(node);
		initializeFigure();
		createHeader();
		addMouseListener(new DoubleClickListener());
	}

	private void initializeFigure() {
		setToolTip(new Label(node.getName()));
		setForegroundColor(Colors.white());
		setBounds(new Rectangle(0, 0, 0, 0));
		setSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);

		GridLayout layout = new GridLayout(1, true);
		layout.horizontalSpacing = 10;
		layout.verticalSpacing = 0;
		layout.marginHeight = MARGIN_HEIGHT;
		layout.marginWidth = MARGIN_WIDTH;
		setLayoutManager(layout);
	}

	private void createHeader() {

		var layout = new GridLayout(4, false);
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 0;
		layout.marginHeight = 2;
		layout.marginWidth = 0;

		var top = new Figure();
		top.setLayoutManager(layout);

		// left expander
		leftExpander = new ProcessExpander(node, Side.INPUT);
		top.add(leftExpander, new GridData(SWT.LEFT, SWT.CENTER, false, false));

		// process icon and header
		top.add(new ImageFigure(Figures.iconOf(node)),
				new GridData(SWT.LEFT, SWT.CENTER, false, false));
		top.add(new BoxHeader(node), new GridData(SWT.FILL, SWT.TOP, true, false));

		// right expander
		rightExpander = new ProcessExpander(node, Side.OUTPUT);
		top.add(rightExpander, new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		add(top, new GridData(SWT.FILL, SWT.FILL, true, false));
	}

	private IOFigure getIOFigure() {
		for (Object child : getChildren())
			if (child instanceof IOFigure)
				return (IOFigure) child;
		return null;
	}

	void refresh() {

		// refresh expanders
		if (leftExpander != null) {
			leftExpander.refresh();
		}
		if (rightExpander != null) {
			rightExpander.refresh();
		}

		// update size
		/*
		if (getParent() == null)
			return;
		var location = getLocation();
		var calculated = calculateSize();
		var current = getSize();
		var bounds = new Rectangle(
				location.x - 1,
				location.y - 1,
				Math.max(calculated.width, current.width),
				node.isMinimized()
					? MINIMUM_HEIGHT
					: Math.max(calculated.height, current.height));
		getParent().setConstraint(this, bounds);
		*/

		// refresh the links of this node
		for (Link link : node.links) {
			if (node.equals(link.inputNode)) {
				link.refreshTargetAnchor();
			} else if (node.equals(link.outputNode)) {
				link.refreshSourceAnchor();
			}
		}
	}

	@Override
	protected void paintFigure(Graphics g) {
		g.pushState();
		g.setBackgroundColor(color);
		g.fillRectangle(new Rectangle(getLocation(), getSize()));
		// paintTop(g);
		if (!node.isMinimized() || Animation.isRunning()) {
			// paintTable(g);
		}
		g.popState();
		super.paintFigure(g);
	}

	private void paintTable(Graphics g) {
		g.setForegroundColor(LINE_COLOR);
		int margin = 5;
		int width = getSize().width;
		int height = getSize().height;
		int x = getLocation().x;
		int y = getLocation().y;
		g.drawLine(new Point(x + margin, y + MINIMUM_HEIGHT
				+ TEXT_HEIGHT + MARGIN_HEIGHT), new Point(x + width - margin,
						y + MINIMUM_HEIGHT + TEXT_HEIGHT + MARGIN_HEIGHT));
		if (height - margin > MINIMUM_HEIGHT + margin)
			g.drawLine(new Point(x + width / 2, y + MINIMUM_HEIGHT + margin), new Point(x + width / 2, y
					+ height - margin));
		g.setForegroundColor(TEXT_COLOR);
		g.drawText(M.Inputs, new Point(x + width / 6, y + MINIMUM_HEIGHT + MARGIN_HEIGHT));
		g.drawText(M.Outputs, new Point(x + 2 * width / 3, y + MINIMUM_HEIGHT + MARGIN_HEIGHT));
		g.setForegroundColor(ColorConstants.black);
	}

	@Override
	protected void paintChildren(Graphics g) {
		super.paintChildren(g);
		var io = getIOFigure();
		if (io == null || !io.isVisible())
			return;
		if (io.intersects(g.getClip(Rectangle.SINGLETON))) {
			g.clipRect(io.getBounds());
			io.paint(g);
			g.restoreState();
		}
	}

	ProcessExpander getLeftExpander() {
		return leftExpander;
	}

	ProcessExpander getRightExpander() {
		return rightExpander;
	}

	@Override
	public Dimension getPreferredSize(int wHint, int hHint) {
		var dim = super.getPreferredSize(wHint, hHint);
		if (dim.width < MINIMUM_WIDTH) {
			return new Dimension(MINIMUM_WIDTH, dim.height);
		}
		return dim;
		/*
		var current = getSize();
		if (node.isMinimized())
			return current;
		var calculated = calculateSize();
		return new Dimension(
				Math.max(calculated.width, current.width),
				Math.max(calculated.height, current.height));
		*/
	}

	/*
	private Dimension calculateSize() {

		int x = MINIMUM_WIDTH;
		if (getSize() != null && getSize().width > x)
			x = getSize().width;

		int y = MINIMUM_HEIGHT;

		if (node != null && !node.isMinimized())
			y = getMinimumHeight();

		return new Dimension(x, y);
	}
	*/

	int getMinimumHeight() {
		return getSize().height;
		/*
		if (minimumHeight != 0)
			return minimumHeight;
		int inputs = 0;
		int outputs = 0;
		for (ExchangeNode e : node.getChildren().get(0).getChildren())
			if (!e.isDummy())
				if (e.exchange.isInput)
					inputs++;
				else
					outputs++;
		int length = Math.max(inputs, outputs);
		int startExchanges = MINIMUM_HEIGHT + 4 * MARGIN_HEIGHT + TEXT_HEIGHT;
		minimumHeight = startExchanges + length * (TEXT_HEIGHT + 1);
		return minimumHeight;
		*/
	}

	private class DoubleClickListener implements MouseListener {

		private boolean firstClick = true;

		@Override
		public void mouseDoubleClicked(MouseEvent arg0) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (e.button == 1) {
				if (firstClick) {
					firstClick = false;
					TimerTask timerTask = new TimerTask() {

						@Override
						public void run() {
							firstClick = true;
						}

					};
					Timer timer = new Timer();
					timer.schedule(timerTask, 250);
				} else {
					ChangeStateCommand command = new ChangeStateCommand(node);
					node.parent().editor.getCommandStack().execute(command);
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent me) {
		}

	}

	private static class BoxHeader extends Figure {

		private final Color background;

		BoxHeader(ProcessNode node) {
			background = Figures.headerBackgroundOf(node);
			setToolTip(new Label(tooltipOf(node)));
			var layout = new GridLayout(1, true);
			layout.marginHeight = 2;
			layout.marginWidth = 10;
			setLayoutManager(layout);
			var label = new Label(node.getName());
			label.setForegroundColor(Figures.headerForegroundOf(node));
			add(label, new GridData(SWT.LEFT, SWT.TOP, true, false));
		}

		String tooltipOf(ProcessNode node) {
			if (node.process instanceof ProcessDescriptor) {
				var d = (ProcessDescriptor) node.process;
				return Labels.of(d.processType) + ": " + node.getName();
			}
			return M.ProductSystem + ": " + node.getName();
		}

		@Override
		protected void paintFigure(Graphics g) {
			g.pushState();
			g.setBackgroundColor(background);
			g.fillRectangle(new Rectangle(getLocation(), getSize()));
			g.popState();
			super.paintFigure(g);
		}
	}

}
