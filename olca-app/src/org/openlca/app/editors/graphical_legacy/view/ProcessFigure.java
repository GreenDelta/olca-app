package org.openlca.app.editors.graphical_legacy.view;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical_legacy.model.Link;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.app.editors.graphical_legacy.themes.Theme.Box;
import org.openlca.app.editors.graphical_legacy.view.ProcessExpander.Side;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;

public class ProcessFigure extends Figure {

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
	private LineBorder border;

	public ProcessFigure(ProcessNode node) {
		this.node = node;
		initializeFigure();
		createHeader();
	}

	private void initializeFigure() {
		setToolTip(new Label(Labels.name(node.process)));
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
		top.add(leftExpander, GridPos.leftCenter());

		// process icon and header
		top.add(new ImageFigure(Images.get(node.process)), GridPos.leftCenter());
		top.add(new BoxHeader(node), GridPos.fillTop());

		// right expander
		rightExpander = new ProcessExpander(node, Side.OUTPUT);
		top.add(rightExpander, GridPos.rightCenter());

		add(top, new GridData(SWT.FILL, SWT.FILL, true, false));

		// box border
		var theme = node.config().theme();
		border = new RoundBorder(theme.boxBorderWidth(Box.of(node)));
		setBorder(border);
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
