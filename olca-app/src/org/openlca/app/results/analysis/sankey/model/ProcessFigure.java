package org.openlca.app.results.analysis.sankey.model;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.M;
import org.openlca.app.results.analysis.sankey.ProcessMouseClick;
import org.openlca.app.util.Colors;
import org.openlca.app.util.DQUI;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.DQSystem;
import org.openlca.util.Strings;

public class ProcessFigure extends Figure {

	public static final int HEIGHT = 120;
	public static final int WIDTH = 200;
	public final ProcessNode node;
	/** Must be disposed when the edit part is deactivated */
	Font boldFont;

	public ProcessFigure(ProcessNode node) {
		setToolTip(new Label(Labels.name(node.product.provider())));
		node.figure = this;
		this.node = node;
		setSize(WIDTH, HEIGHT);
		node.setLayoutConstraints(getBounds());
		addMouseListener(new ProcessMouseClick(node));
		setVisible(false);
	}

	@Override
	protected void paintFigure(Graphics graphics) {
		graphics.pushState();
		double contribution = node.totalShare;
		Color color = Colors.getForContribution(contribution);
		graphics.setBackgroundColor(color);
		paintBody(graphics);
		graphics.popState();
	}

	private void paintBody(Graphics g) {
		Rectangle rect = new Rectangle(getLocation(), getSize());
		g.fillRoundRectangle(rect, 15, 15);
		String singleVal = format(node.directResult);
		String singlePerc = format(node.directShare * 100);
		String totalVal = format(node.totalResul);
		String totalPerc = format(node.totalShare * 100);
		String single = singleVal + " (" + singlePerc + "%)";
		String total = totalVal + " (" + totalPerc + "%)";
		drawTexts(g, single, total);
		drawDqBar(g);
	}

	private void drawTexts(Graphics g, String single, String total) {
		Point loc = getLocation();
		Font normalFont = g.getFont();
		Font boldFont = getBoldFont(normalFont);
		g.setFont(boldFont);
		Color black = g.getForegroundColor();
		g.setForegroundColor(Colors.white());
		String name = Strings.cut(Labels.name(node.product.provider()), 30);
		g.drawText(name, loc.x + 5, loc.y + 5);
		g.setFont(normalFont);
		g.drawText(M.DirectContribution + ":", loc.x + 5, loc.y + 35);
		g.drawText(single, loc.x + 5, loc.y + 50);
		g.drawText(M.UpstreamTotal + ":", loc.x + 5, loc.y + 80);
		g.drawText(total, loc.x + 5, loc.y + 95);
		g.setForegroundColor(black);
	}

	private void drawDqBar(Graphics g) {
		DQResult dqResult = node.parent.editor.dqResult;
		if (!DQUI.displayProcessQuality(dqResult))
			return;
		Point loc = getLocation();
		Dimension size = getSize();
		Color fColor = g.getForegroundColor();
		Color bColor = g.getBackgroundColor();
		g.setForegroundColor(Colors.white());
		g.setBackgroundColor(Colors.white());
		int x = loc.x + size.width - 30;
		int y = loc.y + 10;
		int w = 20;
		DQSystem system = dqResult.setup.processSystem;
		int h = (size.height - 20) / system.indicators.size();
		int[] values = dqResult.get(node.product);
		for (int value : values) {
			Color color = DQUI.getColor(value, system.getScoreCount());
			g.setBackgroundColor(color);
			g.drawRectangle(x, y, w, h);
			g.fillRectangle(x + 1, y + 1, w - 1, h - 1);
			y += h;
		}
		g.setForegroundColor(fColor);
		g.setBackgroundColor(bColor);
	}

	private Font getBoldFont(Font normalFont) {
		if (boldFont != null)
			return boldFont;
		String fontName = normalFont.getFontData()[0].getName();
		int height = normalFont.getFontData()[0].getHeight();
		boldFont = new Font(Display.getCurrent(),
				new FontData[] { new FontData(fontName, height, SWT.BOLD) });
		return boldFont;
	}

	private String format(double val) {
		return Numbers.format(val, 3);
	}

	@Override
	public Dimension getPreferredSize(int hint, int hint2) {
		Dimension size = getSize();
		if (HEIGHT > size.height || WIDTH > size.width)
			return new Dimension(WIDTH, HEIGHT);
		return size;
	}

}
