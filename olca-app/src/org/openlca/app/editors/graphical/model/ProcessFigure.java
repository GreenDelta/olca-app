package org.openlca.app.editors.graphical.model;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
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

	static final int MINIMUM_HEIGHT = 25;
	static final int MINIMUM_WIDTH = 175;
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
		if (node.process instanceof ProcessDescriptor) {
			var d = (ProcessDescriptor) node.process;
			var tooltip = Labels.of(d.processType) + ": " + node.getName();
			setToolTip(new Label(tooltip));
		} else {
			setToolTip(new Label(M.ProductSystem + ": " + node.getName()));
		}

		setForegroundColor(Colors.white());
		setBounds(new Rectangle(0, 0, 0, 0));
		setSize(calculateSize());

		GridLayout layout = new GridLayout(1, true);
		layout.horizontalSpacing = 10;
		layout.verticalSpacing = 0;
		layout.marginHeight = MARGIN_HEIGHT;
		layout.marginWidth = MARGIN_WIDTH;
		setLayoutManager(layout);

		var border = new LineBorder(LINE_COLOR, 1);
		setBorder(border);
	}

	private void createHeader() {

		var layout = new GridLayout(3, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 5;
		layout.marginWidth = 0;

		var top = new Figure();
		top.setLayoutManager(layout);

		leftExpander = new ProcessExpander(node, Side.INPUT);
		top.add(leftExpander, new GridData(SWT.LEFT, SWT.CENTER, false, false));

		var label = new Label(node.getName());
		label.setFont(UI.boldFont());
		label.setBackgroundColor(Colors.gray());
		label.setForegroundColor(Colors.white());
		label.setIcon(Figures.iconOf(node));
		top.add(label, new GridData(SWT.LEFT, SWT.TOP, true, false));

		rightExpander = new ProcessExpander(node, Side.OUTPUT);
		top.add(rightExpander, new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		add(top, new GridData(SWT.FILL, SWT.FILL, true, false));

		// var dummyGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		// dummyGridData.heightHint = MINIMUM_HEIGHT;
		// add(new Figure(), dummyGridData);
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
		if (getParent() == null)
			return;
		var location = getLocation();
		var calculated = calculateSize();
		var current = getSize();
		var bounds = new Rectangle(
			location.x - 1,
			location.y - 1,
			Math.max(calculated.width, current.width),
			Math.max(calculated.height, current.height));
		getParent().setConstraint(this, bounds);

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
		g.setBackgroundColor(Colors.get(0, 0, 102));
		g.fillRectangle(new Rectangle(getLocation(), getSize()));
		// paintTop(g);
		if (!node.isMinimized() || Animation.isRunning()) {
			paintTable(g);
		}
		g.popState();
		super.paintFigure(g);
	}

	private void paintTop(Graphics g) {
		/*
		Image file;
		if (node.isMarked())
			file = Icon.PROCESS_BG_MARKED.get();
		else if (node.process instanceof ProductSystemDescriptor)
			file = Icon.PROCESS_BG_SYS.get();
		else if (node.process.isFromLibrary()) {
			file = Icon.PROCESS_BG_LIB.get();
		} else if (isLCI())
			file = Icon.PROCESS_BG_LCI.get();
		else
			file = Icon.PROCESS_BG.get();
		*/
		int x = getLocation().x;
		int y = getLocation().y;
		int width = getSize().width;

		// for (int i = 0; i < width - 20; i++)
		// 	g.drawImage(file, new Point(x + i, y));

		g.setForegroundColor(LINE_COLOR);
		g.drawLine(
			new Point(x, y + MINIMUM_HEIGHT),
			new Point(x + width - 1, y + MINIMUM_HEIGHT));
		g.setBackgroundColor(Colors.get(0, 0, 102));
		g.setForegroundColor(Colors.white());
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
	public Dimension getPreferredSize(int hint, int hint2) {
		final Dimension cSize = calculateSize();
		if (cSize.height > getSize().height || cSize.width > getSize().width || node.isMinimized())
			return cSize;
		return getSize();
	}

	Dimension calculateSize() {

		int x = MINIMUM_WIDTH;
		if (getSize() != null && getSize().width > x)
			x = getSize().width;

		int y = MINIMUM_HEIGHT;

		if (node != null && !node.isMinimized())
			y = getMinimumHeight();

		return new Dimension(x, y);
	}

	int getMinimumHeight() {
		if (minimumHeight == 0)
			initializeMinimumHeight();
		return minimumHeight;
	}

	private void initializeMinimumHeight() {
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

}
