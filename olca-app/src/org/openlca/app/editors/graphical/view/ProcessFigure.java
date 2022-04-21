package org.openlca.app.editors.graphical.view;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.command.ChangeStateCommand;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.themes.Theme.Box;
import org.openlca.app.editors.graphical.view.ProcessExpander.Side;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;

public class ProcessFigure extends Figure {

	private static final Dimension CORNER_DIMENSION = new Dimension(15, 15);
	public static final int MINIMUM_WIDTH = 250;
	public static int MINIMUM_HEIGHT = 34;
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

	final ProcessNode node;
	private ProcessExpander leftExpander;
	private ProcessExpander rightExpander;
	private Figure processHeader;
	private LineBorder border;
	private LineBorder headerTopBorder;
	private LineBorder headerFullBorder;
	private GridLayout layout;

	public ProcessFigure(ProcessNode node) {
		this.node = node;
		initializeFigure();
		createHeader();
		addMouseListener(new DoubleClickListener());
	}

	private void initializeFigure() {
		setToolTip(new Label(Labels.name(node.process)));
		setForegroundColor(Colors.white());
		layout = new GridLayout(1, true);
		layout.horizontalSpacing = 10;
		layout.verticalSpacing = 0;
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		setLayoutManager(layout);
	}

	private void createHeader() {
		var theme = node.config().theme();
		var borderWidth = theme.boxBorderWidth(Box.of(node));
		// TODO (francois): should be defined in CSS (with probably box.top).
		var topBorderWidth = theme.boxBorderWidth(Box.of(node));

		var arcDifference = new Dimension(
			2 * layout.marginWidth + (borderWidth + topBorderWidth) / 2,
			2 * layout.marginHeight + (borderWidth + topBorderWidth) / 2);

		var boxCorners = Corners.fullRoundedCorners(CORNER_DIMENSION);
		border = new RoundBorder(borderWidth, boxCorners);
		setBorder(border);

		var headerTopCorners = Corners.topRoundedCorners(
			CORNER_DIMENSION.getShrinked(arcDifference));
		headerTopBorder = new RoundBorder(borderWidth, headerTopCorners);
		headerTopBorder.setColor(theme.boxBorderColor(Box.of(node)));
		var headerFullCorners = Corners.fullRoundedCorners(
			CORNER_DIMENSION.getShrinked(arcDifference));
		headerFullBorder = new RoundBorder(borderWidth, headerFullCorners);
		headerFullBorder.setColor(theme.boxBorderColor(Box.of(node)));

		var layout = new GridLayout(4, false);
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 0;
		layout.marginHeight = 2;
		layout.marginWidth = 0;

		processHeader = new Figure();
		processHeader.setLayoutManager(layout);

		// left expander
		leftExpander = new ProcessExpander(node, Side.INPUT);
		processHeader.add(leftExpander, GridPos.leftCenter());

		// process icon and header
		processHeader.add(new ImageFigure(Images.get(node.process)), GridPos.leftCenter());
		processHeader.add(new BoxHeader(node), GridPos.fillTop());

		// right expander
		rightExpander = new ProcessExpander(node, Side.OUTPUT);
		processHeader.add(rightExpander, GridPos.rightCenter());

		// header border
		setHeaderBorder();

		add(processHeader, new GridData(SWT.FILL, SWT.FILL, true, false));
	}

	public void refresh() {

		// refresh expanders
		if (leftExpander != null) {
			leftExpander.refresh();
		}
		if (rightExpander != null) {
			rightExpander.refresh();
		}
		// refresh the links of this node
		for (Link link : node.links) {
			if (node.equals(link.inputNode)) {
				link.refreshTargetAnchor();
			} else if (node.equals(link.outputNode)) {
				link.refreshSourceAnchor();
			}
		}

		// refresh the borders
		setHeaderBorder();
	}

	private void setHeaderBorder() {
		if (node.isMinimized()) {
			processHeader.setBorder(null);
		}
		else {
			processHeader.setBorder(headerTopBorder);
		}
	}

	@Override
	protected void paintFigure(Graphics g) {
		var theme = node.config().theme();
		var box = Box.of(node);
		border.setColor(theme.boxBorderColor(box));
		g.pushState();
		g.setBackgroundColor(theme.boxBackgroundColor(box));
		g.fillRoundRectangle(new Rectangle(getLocation(), getSize()), 15, 15);
		g.popState();
		super.paintFigure(g);
	}

	public ProcessExpander getLeftExpander() {
		return leftExpander;
	}

	public ProcessExpander getRightExpander() {
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

	public int getMinimumHeight() {
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
		private final Box box;
		private final Label label;

		BoxHeader(ProcessNode node) {
			this.node = node;
			this.box = Box.of(node);
			var name = Labels.name(node.process);
			setToolTip(new Label(name));
			var layout = new GridLayout(1, true);
			layout.marginHeight = 2;
			layout.marginWidth = 10;
			setLayoutManager(layout);
			label = new Label(name);
			var theme = node.config().theme();
			label.setForegroundColor(theme.boxFontColor(box));
			add(label, new GridData(SWT.LEFT, SWT.TOP, true, false));
		}

		@Override
		protected void paintFigure(Graphics g) {
			var theme = node.config().theme();
			label.setForegroundColor(theme.boxFontColor(box));
			g.pushState();
			g.setBackgroundColor(theme.boxBackgroundColor(box));
			g.fillRectangle(new Rectangle(getLocation(), getSize()));
			g.popState();
			super.paintFigure(g);
		}
	}
}
