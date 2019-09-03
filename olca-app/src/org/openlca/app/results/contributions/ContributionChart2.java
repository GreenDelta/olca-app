package org.openlca.app.results.contributions;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IBarSeries;
import org.eclipse.swtchart.IBarSeries.BarWidthStyle;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.LineStyle;
import org.eclipse.swtchart.Range;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.FaviColor;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.core.results.ContributionItem;

public class ContributionChart2 {

	private ChartLegend legend;
	private Chart chart;

	public static ContributionChart2 create(Composite parent, FormToolkit tk) {
		Composite comp = UI.formComposite(parent, tk);
		UI.gridLayout(comp, 2);
		UI.gridData(comp, true, true);
		ContributionChart2 cchart = new ContributionChart2();

		// create and configure the chart
		Chart chart = new Chart(comp, SWT.NONE);
		GridData gdata = new GridData(SWT.LEFT, SWT.CENTER, true, true);
		gdata.minimumHeight = 300;
		gdata.minimumWidth = 700;
		chart.setLayoutData(gdata);
		chart.setOrientation(SWT.HORIZONTAL);

		// we set a white title just to fix the problem
		// that the y-axis is cut sometimes
		chart.getTitle().setText(".");
		chart.getTitle().setFont(UI.defaultFont());
		chart.getTitle().setForeground(Colors.white());
		chart.getTitle().setVisible(true);

		IAxis x = chart.getAxisSet().getXAxis(0);
		x.getTitle().setVisible(false);
		x.getTick().setForeground(Colors.darkGray());
		x.enableCategory(true);
		x.setCategorySeries(new String[] { "" });

		IAxis y = chart.getAxisSet().getYAxis(0);
		y.getTitle().setVisible(false);
		y.getTick().setForeground(Colors.darkGray());
		y.getGrid().setStyle(LineStyle.NONE);
		y.getTick().setFormat(new DecimalFormat("0.0E0#",
				new DecimalFormatSymbols(Locale.US)));
		chart.getLegend().setVisible(false);

		cchart.legend = new ChartLegend(comp);
		cchart.chart = chart;
		return cchart;
	}

	public void setLabel(ILabelProvider label) {
		legend.label = label;
	}

	public void setData(List<ContributionItem<?>> items, String unit) {

		// delete the old series
		Arrays.stream(chart.getSeriesSet().getSeries())
				.map(s -> s.getId())
				.forEach(id -> chart.getSeriesSet().deleteSeries(id));

		// select the top 6 items; if there are more than 6 items
		// in the list, select 5 items and calculate a rest.
		Collections.sort(items, new Comparator(true));
		List<ContributionItem<?>> top = items.size() <= 6
				? items
				: items.subList(0, 5);
		Collections.sort(top, new Comparator(false));
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
			IBarSeries bars = (IBarSeries) chart.getSeriesSet()
					.createSeries(SeriesType.BAR, "BS" + i);
			bars.setYSeries(new double[] { top.get(i).amount });
			bars.setBarColor(FaviColor.getForChart(i));
			bars.setBarPadding(15);
			bars.setBarWidth(barWidth);
			bars.setBarWidthStyle(BarWidthStyle.FIXED);

			if (i < (top.size() - 1) || rest != 0) {
				bars = (IBarSeries) chart.getSeriesSet()
						.createSeries(SeriesType.BAR, "BS'" + i);
				bars.setYSeries(new double[] { 0.0 });
				bars.setBarColor(Colors.white());
				bars.setBarPadding(15);
				bars.setBarWidth(barWidth);
				bars.setBarWidthStyle(BarWidthStyle.FIXED);
			}
		}

		// add a rest bar if necessary
		if (rest != 0.0) {
			IBarSeries bars = (IBarSeries) chart.getSeriesSet()
					.createSeries(SeriesType.BAR, "rest");
			bars.setYSeries(new double[] { rest });
			bars.setBarColor(Colors.darkGray());
			bars.setBarPadding(15);
			bars.setBarWidth(barWidth);
			bars.setBarWidthStyle(BarWidthStyle.FIXED);
		}

		// chart.getAxisSet().getYAxis(0).adjustRange();
		setYRange(top, rest);
		legend.setData(top, rest, unit);
		chart.redraw();
	}

	private void setYRange(List<ContributionItem<?>> top, double rest) {
		double min = rest < 0 ? rest : 0;
		double max = rest > 0 ? rest : 0;
		for (ContributionItem<?> item : top) {
			min = Math.min(min, item.amount);
			max = Math.max(max, item.amount);
		}
		min = Rounding.apply(min);
		max = Rounding.apply(max);
		double upper = Math.max(Math.abs(-min), max);
		double lower = min < 0 ? -upper : 0.0;
		chart.getAxisSet().getYAxis(0).setRange(new Range(lower, upper));
		// chart.getAxisSet().getYAxis(0).getTick().setTickMarkStepHint(4);
	}

	private class Comparator implements java.util.Comparator<ContributionItem<?>> {

		private final boolean abs;

		private Comparator(boolean abs) {
			this.abs = abs;
		}

		@Override
		public int compare(ContributionItem<?> o1, ContributionItem<?> o2) {
			double a1 = o1.amount;
			double a2 = o2.amount;
			if (abs) {
				a1 = Math.abs(a1);
				a2 = Math.abs(a2);
			}
			if (a1 == a2)
				return 0;
			if (a1 == 0d)
				return 1;
			if (a2 == 0d)
				return -1;
			return -Double.compare(a1, a2);
		}
	}

}
