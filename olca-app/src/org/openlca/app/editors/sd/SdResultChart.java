package org.openlca.app.editors.sd;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.xygraph.dataprovider.CircularBufferDataProvider;
import org.eclipse.nebula.visualization.xygraph.figures.Trace;
import org.eclipse.nebula.visualization.xygraph.figures.XYGraph;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.sd.eqn.Cell.NumCell;
import org.openlca.sd.eqn.Var;

class SdResultChart {

	private final CircularBufferDataProvider dataProvider;
	public final XYGraph graph;

	public SdResultChart(Composite parent, int height) {
		dataProvider = new CircularBufferDataProvider(false);
		var canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED);
		UI.gridData(canvas, true, true).minimumHeight = height;
		var lws = new LightweightSystem(canvas);
		graph = createGraph(lws);
	}

	public void update(Var var) {
		if (var == null) {
			clearData();
			return;
		}

		var values = var.values();
		if (values.isEmpty()) {
			clearData();
			return;
		}

		double[] xs = new double[values.size()];
		double[] ys = new double[values.size()];
		for (int i = 0; i < values.size(); i++) {
			xs[i] = i + 1;
			var value = values.get(i);
			if (value instanceof NumCell(double v)) {
				ys[i] = v;
			} else {
				ys[i] = 0.0;
			}
		}

		dataProvider.setCurrentXDataArray(xs);
		dataProvider.setCurrentYDataArray(ys);
		updateAxisRanges(xs, ys);
	}

	private void clearData() {
		dataProvider.setCurrentXDataArray(new double[0]);
		dataProvider.setCurrentYDataArray(new double[0]);
		graph.setTitle("No variable selected");
	}

	private void updateAxisRanges(double[] xValues, double[] yValues) {
		if (xValues.length == 0 || yValues.length == 0) {
			return;
		}

		// X-axis: iterations
		var xAxis = graph.getPrimaryXAxis();
		double maxX = xValues[xValues.length - 1];
		xAxis.setRange(0, Math.max(1, maxX + 1));

		// Y-axis: variable values
		var yAxis = graph.getPrimaryYAxis();
		double minY = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;

		for (double y : yValues) {
			if (!Double.isNaN(y) && !Double.isInfinite(y)) {
				minY = Math.min(minY, y);
				maxY = Math.max(maxY, y);
			}
		}

		if (minY != Double.MAX_VALUE && maxY != Double.MIN_VALUE) {
			double range = maxY - minY;
			double margin = range * 0.1; // 10% margin
			if (range == 0) {
				margin = Math.abs(maxY) * 0.1;
				if (margin == 0) {
					margin = 1.0;
				}
			}
			yAxis.setRange(minY - margin, maxY + margin);
		}
	}

	private XYGraph createGraph(LightweightSystem lws) {
		var g = new XYGraph();
		lws.setContents(g);
		g.setShowTitle(true);
		g.setShowLegend(false);
		g.setBackgroundColor(Colors.white());
		g.setTitle("No variable selected");

		// Configure X-axis
		var xAxis = g.getPrimaryXAxis();
		xAxis.setTitle("Iteration");
		xAxis.setRange(0, 10);
		xAxis.setMinorTicksVisible(false);

		// Configure Y-axis
		var yAxis = g.getPrimaryYAxis();
		yAxis.setTitle("Value");
		yAxis.setRange(0, 10);
		yAxis.setMinorTicksVisible(false);
		yAxis.setFormatPattern("###,###,##0.###");

		// Add trace
		var trace = new Trace("Variable", xAxis, yAxis, dataProvider);
		trace.setPointStyle(Trace.PointStyle.CIRCLE);
		trace.setTraceType(Trace.TraceType.SOLID_LINE);
		trace.setTraceColor(Colors.systemColor(SWT.COLOR_BLUE));
		trace.setLineWidth(2);
		g.addTrace(trace);

		return g;
	}
}
