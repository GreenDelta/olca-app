package org.openlca.app.results.simulation;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.M;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Numbers;
import org.openlca.core.results.Statistics;
import org.openlca.core.results.Statistics.Histogram;

/**
 * Draws a chart with a frequency distribution and statistic parameters.
 */
public class StatisticFigure extends Figure {

	private Histogram hist = Statistics.hist(new double[0], 100);

	private final int marginLeft = 35;
	private final int marginBottom = 35;
	private final int marginRight = 25;
	private final int marginTop = 25;

	private final Label numberLabel;
	private final Label perc5Label;
	private final Label perc95Label;
	private final Label medianLabel;
	private final Label meanLabel;
	private final Label standardDevLabel;

	private final Theme t = new Theme();

	public StatisticFigure() {
		setOpaque(true);
		setBackgroundColor(t.background());
		setForegroundColor(t.foreground());
		setBorder(new LineBorder(t.foreground(), 1));
		GridLayout gl = new GridLayout();
		gl.numColumns = 12;
		setLayoutManager(gl);
		numberLabel = initLabel(M.Results);
		meanLabel = initLabel(M.Mean);
		standardDevLabel = initLabel(M.StandardDeviation);
		perc5Label = initLabel(M.Percentile5);
		perc95Label = initLabel(M.Percentile95);
		medianLabel = initLabel(M.Median);
	}

	private Label initLabel(String text) {
		var label = new Label(text + ":");
		label.setForegroundColor(t.header());
		var value = new Label("0");
		value.setForegroundColor(t.foreground());
		add(label);
		add(value);
		return value;
	}

	void setData(double[] values) {
		hist = Statistics.hist(values, 100);
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		g.pushState();
		paintParameterLabels();
		paintChartFrame(g);
		Point boxSize = calcBoxSize();
		paintBoxes(g, boxSize);
		paintLines(g, boxSize);
		g.popState();
	}

	private void paintBoxes(Graphics g, Point size) {
		g.setBackgroundColor(t.boxFill());
		int height = getSize().height - marginBottom;
		for (int interval = 0; interval < 100; interval++) {
			int frequency = hist.getAbsoluteFrequency(interval);
			for (int block = 1; block <= frequency; block++) {
				int x = marginLeft + interval * size.x;
				int y = height - block * size.y;
				if (y >= marginTop) {
					g.drawRectangle(x, y, size.x, size.y);
					g.fillRectangle(x + 1, y + 1, size.x - 1, size.y - 1);
				}
			}
		}
		g.setBackgroundColor(t.background());
	}


	private void paintParameterLabels() {
		numberLabel.setText(Integer.toString(hist.statistics.count));
		setLabelValue(perc5Label, hist.statistics.getPercentileValue(5));
		setLabelValue(perc95Label, hist.statistics.getPercentileValue(95));
		setLabelValue(medianLabel, hist.statistics.median);
		setLabelValue(meanLabel, hist.statistics.mean);
		setLabelValue(standardDevLabel, hist.statistics.standardDeviation);
	}

	private void paintChartFrame(Graphics g) {
		g.drawLine(marginLeft, getSize().height - marginBottom,
			getSize().width - marginRight, getSize().height - marginBottom);
		g.drawLine(marginLeft, marginTop, marginLeft, getSize().height
			- marginBottom);
		g.drawText(Numbers.format(hist.statistics.min, 3),
			marginLeft, getSize().height - marginBottom + 10);
		g.drawText(Numbers.format(hist.statistics.max, 3),
			getSize().width - marginRight - 40, getSize().height
				- marginBottom + 10);
		g.drawText(
			Integer.toString(hist.getMaxAbsoluteFrequency()), 15,
			marginTop + 5);
		g.drawText("0", 15, getSize().height - marginBottom - 15);
	}

	private Point calcBoxSize() {
		Point size = new Point();
		int width = getSize().width - marginLeft - marginRight;
		int height = getSize().height - marginTop - marginBottom;
		if (width <= 0 || height <= 0) {
			size.x = 0;
			size.y = 0;
			return size;
		}
		int intervalCount = 100;
		int maxFreq = hist.getMaxAbsoluteFrequency();
		if (maxFreq > height) {
			double factor = (double) maxFreq / (double) height;
			maxFreq /= ((int) factor);
		}
		maxFreq = maxFreq == 0 ? 1 : maxFreq;
		size.x = width / intervalCount;
		size.y = height / maxFreq;
		if (size.x < 1)
			size.x = 1;
		if (size.y < 1)
			size.y = 1;
		return size;
	}

	private void paintLines(Graphics g, Point box) {
		g.setForegroundColor(ColorConstants.red);
		drawLine(g, hist.statistics.getPercentileValue(5), box);
		drawLine(g, hist.statistics.median, box);
		drawLine(g, hist.statistics.getPercentileValue(95), box);
		drawLine(g, hist.statistics.mean, box);
		g.setForegroundColor(t.foreground());
	}

	private void drawLine(Graphics g, double val, Point box) {
		int interval = hist.getInterval(val);
		int x = box.x * interval + marginLeft + box.x / 2;
		g.drawLine(x, getSize().height - marginBottom, x, marginTop);
	}

	private void setLabelValue(Label label, double val) {
		label.setText(Numbers.format(val, 3));
	}

	private static class Theme {

		private final boolean isDark;

		private Theme() {
			this.isDark = Display.isSystemDarkTheme();
		}

		Color background() {
			return Colors.background();
		}

		Color foreground() {
			return isDark ? ColorConstants.white : ColorConstants.black;
		}

		Color header() {
			return isDark ? ColorConstants.yellow : ColorConstants.blue;
		}

		Color boxFill() {
			return isDark ? ColorConstants.gray : ColorConstants.lightGray;
		}
	}

}
