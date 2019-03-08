package org.openlca.app.results.simulation;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.swt.widgets.Composite;

/**
 * A canvas for displaying uncertainty statistics.
 */
public class StatisticsCanvas extends FigureCanvas {

	private StatisticFigure plot;

	public StatisticsCanvas(Composite parent) {
		super(parent);
		plot = new StatisticFigure();
		setContents(plot);
	}

	public void setValues(double[] values) {
		plot.setData(values);
	}
}
