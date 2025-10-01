package org.openlca.app.editors.sd.results;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.xygraph.dataprovider.CircularBufferDataProvider;
import org.eclipse.nebula.visualization.xygraph.figures.Trace;
import org.eclipse.nebula.visualization.xygraph.figures.XYGraph;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.sd.eqn.Var;
import org.openlca.sd.eqn.cells.NumCell;

class SdResultChart {

	private final CircularBufferDataProvider buffer;
	public final XYGraph graph;

	public SdResultChart(Composite parent, int height) {
		buffer = new CircularBufferDataProvider(false);
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

		buffer.setBufferSize(xs.length);
		buffer.setCurrentXDataArray(xs);
		buffer.setCurrentYDataArray(ys);
		updateAxisRanges(xs, ys);
	}

	private void clearData() {
		buffer.setCurrentXDataArray(new double[0]);
		buffer.setCurrentYDataArray(new double[0]);
	}

	private void updateAxisRanges(double[] xs, double[] ys) {
		if (xs.length == 0 || ys.length == 0)
			return;

		// X-axis: iterations
		var xAxis = graph.getPrimaryXAxis();
		double maxX = xs[xs.length - 1];
		xAxis.setRange(0, Math.max(1, maxX + 1));

		// Y-axis: variable values
		var yAxis = graph.getPrimaryYAxis();
		double minY = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;
		for (double y : ys) {
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
		}

		double range = maxY - minY;
		double margin = range * 0.1; // 10% margin
		if (range == 0) {
			margin = Math.abs(maxY) * 0.1;
			if (margin == 0) {
				margin = 1.0;
			}
		}

		yAxis.setRange(
				minY > 0 ? 0 : minY - margin
				, maxY + margin);
	}

	private XYGraph createGraph(LightweightSystem lws) {
		var g = new XYGraph();
		lws.setContents(g);
		g.setShowTitle(true);
		g.setShowLegend(false);
		g.setBackgroundColor(Colors.white());
		g.setTitle("");

		// Configure X-axis
		var xAxis = g.getPrimaryXAxis();
		xAxis.setRange(0, 10);
		xAxis.setMinorTicksVisible(false);
		xAxis.setTitle("");

		// Configure Y-axis
		var yAxis = g.getPrimaryYAxis();
		yAxis.setRange(0, 10);
		yAxis.setMinorTicksVisible(false);
		yAxis.setFormatPattern("###,###,##0.###");
		yAxis.setTitle("");

		// Add trace
		var trace = new Trace("Variable", xAxis, yAxis, buffer);
		trace.setPointStyle(Trace.PointStyle.NONE);
		trace.setTraceType(Trace.TraceType.SOLID_LINE);
		trace.setTraceColor(Colors.fromHex("#3F51B5"));
		trace.setLineWidth(2);
		g.addTrace(trace);
		return g;
	}
}
