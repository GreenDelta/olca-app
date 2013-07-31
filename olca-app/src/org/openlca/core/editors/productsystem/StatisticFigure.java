package org.openlca.core.editors.productsystem;

import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.util.Numbers;
import org.openlca.core.math.Statistics;

/**
 * Draws a chart with a frequency distribution and statistic parameters.
 */
public class StatisticFigure extends Figure {

	private Statistics statistics = Statistics.empty();

	private int marginLeft = 35;
	private int marginBottom = 35;
	private int marginRight = 25;
	private int marginTop = 25;

	private Label numberLabel;
	private Label perc5Label;
	private Label perc95Label;
	private Label medianLabel;
	private Label meanLabel;
	private Label standardDevLabel;

	public StatisticFigure() {
		setOpaque(true);
		setBackgroundColor(ColorConstants.white);
		setForegroundColor(ColorConstants.black);
		setBorder(new LineBorder(ColorConstants.black, 1));
		GridLayout gl = new GridLayout();
		gl.numColumns = 12;
		setLayoutManager(gl);
		numberLabel = initLabel("results");
		meanLabel = initLabel("mean");
		standardDevLabel = initLabel("standard deviation");
		perc5Label = initLabel("5% percentile");
		perc95Label = initLabel("95% percentile");
		medianLabel = initLabel("median");
	}

	private Label initLabel(String text) {
		Label textLabel = new Label(text + ":");
		textLabel.setForegroundColor(ColorConstants.blue);
		Label valueLabel = new Label("0");
		valueLabel.setForegroundColor(ColorConstants.black);
		add(textLabel);
		add(valueLabel);
		return valueLabel;
	}

	public void setData(List<Double> values) {
		statistics = new Statistics(values, 100);
		repaint();
	}

	@Override
	public void paint(Graphics graphics) {
		super.paint(graphics);
		graphics.pushState();
		paintParameterLabels();
		paintChartFrame(graphics);
		Point boxSize = calcBoxSize();
		paintBoxes(graphics, boxSize);
		paintLines(graphics, boxSize);
		graphics.popState();
	}

	private void paintBoxes(Graphics graphics, Point boxSize) {
		graphics.setBackgroundColor(ColorConstants.lightGray);
		int height = getSize().height - marginBottom;
		for (int interval = 0; interval < 100; interval++) {
			int frequency = statistics.getAbsoluteFrequency(interval);
			for (int block = 1; block <= frequency; block++) {
				int x = marginLeft + interval * boxSize.x;
				int y = height - block * boxSize.y;
				if (y >= marginTop)
					drawBox(graphics, boxSize, new Point(x, y));
			}
		}
		graphics.setBackgroundColor(ColorConstants.white);
	}

	private void drawBox(Graphics graphics, Point boxSize, Point topLeft) {
		graphics.drawRectangle(topLeft.x, topLeft.y, boxSize.x, boxSize.y);
		graphics.fillRectangle(topLeft.x + 1, topLeft.y + 1, boxSize.x - 1,
				boxSize.y - 1);
	}

	private void paintParameterLabels() {
		numberLabel.setText(statistics.getCount() + "");
		setLabelValue(perc5Label, statistics.getPercentileValue(5));
		setLabelValue(perc95Label, statistics.getPercentileValue(95));
		setLabelValue(medianLabel, statistics.getMedian());
		setLabelValue(meanLabel, statistics.getMean());
		setLabelValue(standardDevLabel, statistics.getStandardDeviation());
	}

	private void paintChartFrame(Graphics graphics) {
		graphics.drawLine(marginLeft, getSize().height - marginBottom,
				getSize().width - marginRight, getSize().height - marginBottom);
		graphics.drawLine(marginLeft, marginTop, marginLeft, getSize().height
				- marginBottom);
		graphics.drawText(Numbers.format(statistics.getMinimum(), 3),
				marginLeft, getSize().height - marginBottom + 10);
		graphics.drawText(Numbers.format(statistics.getMaximum(), 3),
				getSize().width - marginRight - 40, getSize().height
						- marginBottom + 10);
		graphics.drawText(
				Integer.toString(statistics.getMaximalAbsoluteFrequency()), 15,
				marginTop + 5);
		graphics.drawText("0", 15, getSize().height - marginBottom - 15);
	}

	private Point calcBoxSize() {
		Point size = new Point();
		int width = getSize().width - marginLeft - marginRight;
		int height = getSize().height - marginTop - marginBottom;
		int intervalCount = 100;
		int maxFreq = statistics.getMaximalAbsoluteFrequency();
		if (maxFreq > height) {
			double factor = (double) maxFreq / (double) height;
			maxFreq /= factor;
		}
		size.x = width / intervalCount;
		size.y = height / maxFreq;
		if (size.x < 1)
			size.x = 1;
		if (size.y < 1)
			size.y = 1;
		return size;
	}

	private void paintLines(Graphics graphics, Point boxSize) {
		graphics.setForegroundColor(ColorConstants.red);
		drawLine(graphics, statistics.getPercentileValue(5), boxSize);
		drawLine(graphics, statistics.getMedian(), boxSize);
		drawLine(graphics, statistics.getPercentileValue(95), boxSize);
		drawLine(graphics, statistics.getMean(), boxSize);
		graphics.setForegroundColor(ColorConstants.black);
	}

	private void drawLine(Graphics graphics, double val, Point boxSize) {
		int interval = statistics.getInterval(val);
		int x = boxSize.x * interval + marginLeft + boxSize.x / 2;
		graphics.drawLine(x, getSize().height - marginBottom, x, marginTop);
	}

	private void setLabelValue(Label label, double val) {
		label.setText(Numbers.format(val, 3));
	}

}
