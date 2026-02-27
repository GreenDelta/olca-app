package org.openlca.app.editors.sd.results;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.xygraph.dataprovider.CircularBufferDataProvider;
import org.eclipse.nebula.visualization.xygraph.figures.Trace;
import org.eclipse.nebula.visualization.xygraph.figures.XYGraph;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.openlca.sd.interop.CoupledResult;
import org.openlca.app.preferences.Theme;
import org.openlca.app.util.Colors;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.sd.model.Var;

class ResultChart {

	private final CoupledResult result;
	private final double[] time;
	private final XYGraph graph;
	private final List<Trace> traces = new ArrayList<>();

	ResultChart(Composite parent, CoupledResult result) {
		this.result = result;
		this.time = result.timeLine();
		var canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED);
		canvas.setBackground(Colors.background());
		UI.gridData(canvas, true, true).minimumHeight = 300;
		var lws = new LightweightSystem(canvas);
		graph = createGraph(lws);
	}

	void show(Var var) {
		clear();

		var seqs = ChartSeq.of(result, var);
		if (seqs.isEmpty())
			return;

		if (seqs.size() == 1) {
			var seq = seqs.getFirst();
			var trace = createTrace(seq.title(), seq.values());
			trace.setTraceType(Trace.TraceType.SOLID_LINE);
			var range = ChartRange.of(seq.values());
			graph.getPrimaryYAxis()
				.setRange(range.min(), range.max());
			return;
		}

		ChartRange range = null;
		for (int i = 0; i < seqs.size(); i++) {
			var seq = seqs.get(i);
			var trace = createTrace(seq.title(), seq.values());
			trace.setTraceType(Trace.TraceType.SOLID_LINE);
			trace.setTraceColor(Colors.getForChart(i));
			var r = ChartRange.of(seq.values());
			range = range == null
				? r
				: new ChartRange(
					Math.min(r.min(), range.min()),
					Math.max(r.max(), range.max()));
		}
		if (range != null) {
			graph.getPrimaryYAxis().setRange(range.min(), range.max());
		}
	}

	void show(ImpactDescriptor d) {
		clear();
		double[] ys = result.impactResultsOf(d);

		var range = ChartRange.of(ys);
		graph.getPrimaryYAxis()
			.setRange(range.min(), range.max());

		var trace = createTrace(d.name, ys);
		trace.setTraceType(Trace.TraceType.LINE_AREA);
		trace.setAreaAlpha(50);
		trace.setLineWidth(2);
	}

	private Trace createTrace(String title, double[] nums) {

		var buffer = new CircularBufferDataProvider(false);
		buffer.setBufferSize(nums.length);
		buffer.setCurrentXDataArray(time);
		buffer.setCurrentYDataArray(nums);

		var x = graph.getPrimaryXAxis();
		var y = graph.getPrimaryYAxis();

		var trace = new Trace(title, x, y, buffer);
		trace.setToolTip(new Label(title));
		var defaultColor = Theme.isDark()
			? Colors.getForChart(2)
			: Colors.getForChart(1);
		trace.setTraceColor(defaultColor);
		trace.setPointStyle(Trace.PointStyle.NONE);
		trace.setLineWidth(2);
		traces.add(trace);
		graph.addTrace(trace);
		return trace;
	}

	private void clear() {
		for (var trace : traces) {
			try {
				graph.removeTrace(trace);
				trace.dispose();
			} catch (Exception e) {
				ErrorReporter.on("Failed to remove data traces", e);
			}
		}
		traces.clear();
	}

	private XYGraph createGraph(LightweightSystem lws) {
		var g = new XYGraph();
		lws.setContents(g);
		g.setShowTitle(true);
		g.setShowLegend(false);
		g.setBackgroundColor(Colors.background());
		g.setTitle("");
		g.getPlotArea().setBackgroundColor(Colors.background());

		// configure x
		var start = time.length > 1 ? time[0] : 0;
		var end = time.length > 0 ? time[time.length - 1] : 1;
		var x = g.getPrimaryXAxis();
		x.setRange(start, end);
		x.setMinorTicksVisible(false);
		x.setTitle("");

		// configure y
		var y = g.getPrimaryYAxis();
		y.setRange(0, 1);
		y.setMinorTicksVisible(false);
		y.setFormatPattern("###,###,##0.###");
		y.setTitle("");

		return g;
	}
}
