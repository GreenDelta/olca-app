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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.FaviColor;
import org.openlca.app.M;
import org.openlca.app.results.analysis.sankey.ProcessMouseClick;
import org.openlca.app.util.Colors;
import org.openlca.app.util.DQUIHelper;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.util.Strings;

public class ProcessFigure extends Figure {

	public static final int HEIGHT = 120;
	public static final int WIDTH = 200;
	private ProcessNode processNode;
	private Font boldFont;

	public ProcessFigure(ProcessNode processNode) {
		setToolTip(new Label(Labels.getDisplayName(processNode.process)));
		processNode.figure = this;
		this.processNode = processNode;
		setSize(WIDTH, HEIGHT);
		processNode.setXyLayoutConstraints(getBounds());
		addMouseListener(new ProcessMouseClick(processNode));
	}

	/** Must be disposed when the edit part is deactivated */
	Font getBoldFont() {
		return boldFont;
	}

	@Override
	protected void paintFigure(Graphics graphics) {
		graphics.pushState();
		graphics.setBackgroundColor(getColor());
		paintBody(graphics);
		graphics.popState();
	}

	private Color getColor() {
		double contribution = processNode.upstreamContribution;
		RGB rgb = FaviColor.getForContribution(contribution);
		return Colors.get(rgb);
	}

	private void paintBody(Graphics g) {
		Rectangle rect = new Rectangle(getLocation(), getSize());
		g.fillRoundRectangle(rect, 15, 15);
		String singleVal = format(processNode.directResult);
		String singlePerc = format(processNode.directContribution * 100);
		String totalVal = format(processNode.upstreamResult);
		String totalPerc = format(processNode.upstreamContribution * 100);
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
		String name = Strings.cut(processNode.getName(), 30);
		g.drawText(name, loc.x + 5, loc.y + 5);
		g.setFont(normalFont);
		g.drawText(M.DirectContribution + ":", loc.x + 5, loc.y + 35);
		g.drawText(single, loc.x + 5, loc.y + 50);
		g.drawText(M.UpstreamTotal + ":", loc.x + 5, loc.y + 80);
		g.drawText(total, loc.x + 5, loc.y + 95);
		g.setForegroundColor(black);
	}

	private void drawDqBar(Graphics g) {
		DQResult dqResult = ((ProductSystemNode) processNode.getParent()).getEditor().getDqResult();
		if (!DQUIHelper.displayProcessQuality(dqResult))
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
		int h = (size.height - 20) / dqResult.processSystem.indicators.size();
		double[] values = dqResult.get(processNode.process);
		for (int i = 0; i < values.length; i++) {
			Color color = DQUIHelper.getColor(values[i], dqResult.processSystem.getScoreCount(), dqResult.rounding);
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

	public ProcessNode getProcessNode() {
		return processNode;
	}

}
