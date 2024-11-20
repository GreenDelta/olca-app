package org.openlca.app.results.analysis.groups;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IBarSeries;
import org.eclipse.swtchart.IBarSeries.BarWidthStyle;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.LineStyle;
import org.eclipse.swtchart.Range;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

class GroupChart {

	private final Chart chart;

	private GroupChart(Chart chart) {
		this.chart = chart;
	}

	static GroupChart create(Composite comp, FormToolkit tk) {
		// chart
		var chart = new Chart(comp, SWT.NONE);
		var gdata = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		gdata.heightHint = 300;
		gdata.widthHint = 700;
		chart.setLayoutData(gdata);
		chart.setOrientation(SWT.HORIZONTAL);
		chart.getLegend().setVisible(false);

		// we set a white title just to fix the problem
		// that the y-axis is cut sometimes
		chart.getTitle().setText(".");
		chart.getTitle().setFont(UI.defaultFont());
		chart.getTitle().setForeground(Colors.background());
		chart.getTitle().setVisible(true);

		// configure the x-axis with one category
		var x = chart.getAxisSet().getXAxis(0);
		x.getTitle().setVisible(false);
		x.getTick().setForeground(Colors.darkGray());
		x.enableCategory(true);
		x.setCategorySeries(new String[]{""});

		// configure the y-axis
		var y = chart.getAxisSet().getYAxis(0);
		y.getTitle().setVisible(false);
		y.getTick().setForeground(Colors.darkGray());
		y.getGrid().setStyle(LineStyle.NONE);
		y.getTick().setFormat(new DecimalFormat("0.0E0#",
				new DecimalFormatSymbols(Locale.US)));
		y.getTick().setTickMarkStepHint(10);

		tk.adapt(chart);
		return new GroupChart(chart);
	}

	void setInput(List<GroupValue> values) {

		// delete the old series
		Arrays.stream(chart.getSeriesSet().getSeries())
				.map(ISeries::getId)
				.forEach(id -> chart.getSeriesSet().deleteSeries(id));

		// calculate the bar width
		int n = values.size();
		int barWidth = ((int) (600.0 / 11.0)) * (2 * n - 1);

		// create the new bars
		for (int i = 0; i < values.size(); i++) {
			var v = values.get(i);
			var color = Strings.notEmpty(v.group().color)
					? Colors.fromHex(v.group().color)
					: Colors.getForChart(i);
			createBar("BS" + i, v.value(), color, barWidth);
			if (i < (values.size() - 1)) {
				createBar("BS" + i, 0.0, Colors.white(), barWidth);
			}
		}

		setYRange(values);
		chart.redraw();
	}

	private void createBar(String id, double val, Color color, int width) {
		var bars = (IBarSeries<?>) chart.getSeriesSet()
				.createSeries(SeriesType.BAR, id);
		bars.setYSeries(new double[] { val });
		bars.setBarColor(color);
		bars.setBarPadding(15);
		bars.setBarWidth(width);
		bars.setBarWidthStyle(BarWidthStyle.FIXED);
	}

	private void setYRange(List<GroupValue> values) {
		double min = 0;
		double max = 0;
		for (var v : values) {
			min = Math.min(min, v.value());
			max = Math.max(max, v.value());
		}
		double[] range = yrange(min, max);
		IAxis y = chart.getAxisSet().getYAxis(0);
		y.setRange(new Range(range[0], range[1]));
		y.getTick().setTickMarkStepHint(10);
	}

	private double[] yrange(double min, double max) {
		// find the dimension on the Log10 scale
		double absmax = Math.max(Math.abs(min), Math.abs(max));
		if (absmax == 0) {
			return new double[]{0, 1};
		}
		double dim = Math.ceil(Math.log10(absmax));

		// iterate from the top in a number of steps
		// to find the optimal chart scale
		double top = Math.pow(10, dim);
		double step = top / 50.0;
		while ((top - step) > absmax) {
			top -= step;
		}

		// return the scale depending on the sign
		// of the maximum and minimum values
		if (min >= 0 && max >= 0)
			return new double[]{0, top};
		if (min <= 0 && max <= 0)
			return new double[]{-top, 0};
		else
			return new double[]{-top, top};
	}
}
