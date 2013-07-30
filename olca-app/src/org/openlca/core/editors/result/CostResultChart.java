package org.openlca.core.editors.result;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Colors;
import org.openlca.core.application.FaviColor;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.IBarSeries;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.ISeriesSet;
import org.swtchart.LineStyle;

class CostResultChart {

	private double[] values;
	private String[] categories;

	public CostResultChart(List<CostResultItem> results) {
		calculateValues(results);
	}

	private void calculateValues(List<CostResultItem> results) {
		int resultSize = results.size();
		values = new double[resultSize + 1];
		categories = new String[resultSize + 1];
		double sum = 0;
		for (int i = 0; i < resultSize; i++) {
			CostResultItem item = results.get(i);
			values[i] = item.getAmount();
			categories[i] = item.getCostCategory().getName();
			sum += item.getAmount();
		}
		values[resultSize] = sum;
		categories[resultSize] = "Total";
	}

	public void render(Composite parent) {
		Chart chart = new Chart(parent, SWT.NONE);
		chart.getTitle().setText("Cost results");
		chart.getTitle().setForeground(Colors.getBlack());
		chart.getLegend().setVisible(false);
		chart.setBackground(Colors.getWhite());
		chart.setForeground(Colors.getBlack());
		createSeries(chart);
		configureX(chart);
		configureY(chart);
		chart.getAxisSet().adjustRange();
	}

	private void createSeries(Chart chart) {
		ISeriesSet seriesSet = chart.getSeriesSet();
		IBarSeries series = (IBarSeries) seriesSet.createSeries(SeriesType.BAR,
				"s");
		series.setYSeries(values);
		series.setBarColor(FaviColor.getForChart(1));
		series.setBarPadding(50);
	}

	private void configureX(Chart chart) {
		IAxis xAxis = chart.getAxisSet().getXAxis(0);
		xAxis.setCategorySeries(categories);
		xAxis.enableCategory(true);
		xAxis.getTick().setForeground(Colors.getBlack());
		xAxis.getTick().setTickLabelAngle(45);
		xAxis.getTitle().setVisible(false);
		xAxis.getGrid().setStyle(LineStyle.NONE);
	}

	private void configureY(Chart chart) {
		IAxis yAxis = chart.getAxisSet().getYAxis(0);
		yAxis.getTick().setForeground(Colors.getBlack());
		yAxis.getGrid().setStyle(LineStyle.NONE);
		yAxis.getTitle().setVisible(false);
		yAxis.getTitle().setForeground(Colors.getBlack());
	}

}
