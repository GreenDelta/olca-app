package org.openlca.app.results.contributions;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.viewers.ILabelProvider;
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
import org.openlca.core.results.Contribution;

public class ContributionChart {

	private final ChartLegend legend;
	private final Chart chart;

	public static ContributionChart create(Composite parent, FormToolkit tk) {
		var comp = UI.composite(parent, tk);
		UI.gridLayout(comp, 2);
		UI.gridData(comp, true, true);

		// create and configure the chart
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
		x.setCategorySeries(new String[] { "" });

		// configure the y-axis
		var y = chart.getAxisSet().getYAxis(0);
		y.getTitle().setVisible(false);
		y.getTick().setForeground(Colors.darkGray());
		y.getGrid().setStyle(LineStyle.NONE);
		y.getTick().setFormat(new DecimalFormat("0.0E0#",
				new DecimalFormatSymbols(Locale.US)));
		y.getTick().setTickMarkStepHint(10);

		tk.adapt(chart);
		return new ContributionChart(chart, new ChartLegend(comp, tk));
	}

	private ContributionChart(Chart chart, ChartLegend legend) {
		this.chart = chart;
		this.legend = legend;
	}

	public void setLabel(ILabelProvider label) {
		legend.label = label;
	}

	public void setData(List<? extends Contribution<?>> items, String unit) {

		// delete the old series
		Arrays.stream(chart.getSeriesSet().getSeries())
				.map(ISeries::getId)
				.forEach(id -> chart.getSeriesSet().deleteSeries(id));

		// select the top 6 items; if there are more than 6 items
		// in the list, select 5 items and calculate a rest.
		// note that we first rank the items by absolute values to
		// get the top contributers but then sort them by their
		// real values to have a nice order in the chart.
		items.sort((i1, i2) -> -Double.compare(
				Math.abs(i1.amount), Math.abs(i2.amount)));
		var top = items.size() <= 6
				? items
				: items.subList(0, 5);
		top.sort((i1, i2) -> -Double.compare(
				i1.amount, i2.amount));
		double rest = 0;
		if (items.size() > 6) {
			for (int i = 5; i < items.size(); i++) {
				rest += items.get(i).amount;
			}
		}

		// calculate the bar width
		int n = top.size();
		if (rest != 0) {
			n += 1;
		}
		int barWidth = ((int) (600.0 / 11.0)) * (2 * n - 1);

		// create the new series
		for (int i = 0; i < top.size(); i++) {
			createBar("BS" + i, top.get(i).amount,
					Colors.getForChart(i), barWidth);
			if (i < (top.size() - 1) || rest != 0) {
				// create an empty space bar
				createBar("BS''" + i, 0.0, Colors.white(), barWidth);
			}
		}
		// add a rest bar if necessary
		if (rest != 0.0) {
			createBar("rest", rest, Colors.darkGray(), barWidth);
		}

		setYRange(top, rest);
		legend.setData(top, rest, unit);
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

	private void setYRange(List<? extends Contribution<?>> top, double rest) {
		double min = rest < 0 ? rest : 0;
		double max = rest > 0 ? rest : 0;
		for (var item : top) {
			min = Math.min(min, item.amount);
			max = Math.max(max, item.amount);
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
			return new double[] { 0, 1 };
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
			return new double[] { 0, top };
		if (min <= 0 && max <= 0)
			return new double[] { -top, 0 };
		else
			return new double[] { -top, top };
	}
}
