package org.openlca.app.editors.graphical.model;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.command.ChangeStateCommand;
import org.openlca.app.editors.graphical.model.ProcessExpander.Side;
import org.openlca.app.rcp.images.Icon;
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

	final ProcessNode node;

	private ProcessExpander leftExpander;
	private ProcessExpander rightExpander;
	private LineBorder border;

	ProcessFigure(ProcessNode node) {
		this.node = node;
		initializeFigure();
		createHeader();
		addMouseListener(new DoubleClickListener());
	}

	private void initializeFigure() {
		setToolTip(new Label(node.getName()));
		setForegroundColor(Colors.white());
		var layout = new GridLayout(1, true);
		layout.horizontalSpacing = 10;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
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
		top.add(new ImageFigure(Icon.GRAPH_PROCESS_PRODUCTION.get()),
				new GridData(SWT.LEFT, SWT.CENTER, false, false));
		top.add(new BoxHeader(node), new GridData(SWT.FILL, SWT.TOP, true, false));

		// right expander
		rightExpander = new ProcessExpander(node, Side.OUTPUT);
		top.add(rightExpander, new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		add(top, new GridData(SWT.FILL, SWT.FILL, true, false));

		border = new LineBorder(1);
		setBorder(border);
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
		var theme = node.config().theme();
		border.setColor(theme.borderColorOf(node));
		g.pushState();
		g.setBackgroundColor(theme.backgroundColorOf(node));
		g.fillRectangle(new Rectangle(getLocation(), getSize()));
		g.popState();
		super.paintFigure(g);
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
		if (wHint < 0) {
			dim = new Dimension(MINIMUM_WIDTH, dim.height);
		}
		var size = getSize();
		if (size.width == 0 || size.height == 0) {
			setSize(dim);
		}
		return dim;
	}

	int getMinimumHeight() {
		return getSize().height;
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

		private final ProcessNode node;
		private final Label label;

		BoxHeader(ProcessNode node) {
			this.node = node;
			var theme = node.config().theme();
			setToolTip(new Label(tooltipOf(node)));
			var layout = new GridLayout(1, true);
			layout.marginHeight = 2;
			layout.marginWidth = 10;
			setLayoutManager(layout);
			label = new Label(node.getName());
			label.setForegroundColor(theme.fontColorOf(node));
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
			var theme = node.config().theme();
			label.setForegroundColor(theme.fontColorOf(node));
			g.pushState();
			g.setBackgroundColor(theme.backgroundColorOf(node));
			g.fillRectangle(new Rectangle(getLocation(), getSize()));
			g.popState();
			super.paintFigure(g);
		}
	}

}
