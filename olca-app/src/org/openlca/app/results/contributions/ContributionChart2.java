package org.openlca.app.results.contributions;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IBarSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.LineStyle;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.FaviColor;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;

public class ContributionChart2 {

	private ChartLegend legend;

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
		chart.getLegend().setVisible(false);

		for (int i = 0; i < 6; i++) {
			IBarSeries bars = (IBarSeries) chart.getSeriesSet()
					.createSeries(SeriesType.BAR, "BS" + i);
			bars.setYSeries(new double[] { Math.random() * 100 });
			bars.setBarColor(FaviColor.getForChart(i));
			bars.setBarPadding(15);

			if (i < 5) {
				bars = (IBarSeries) chart.getSeriesSet()
						.createSeries(SeriesType.BAR, "BS'" + i);
				bars.setYSeries(new double[] { 0.0 });
				bars.setBarColor(Colors.white());
				bars.setBarPadding(15);
			}
		}

		y.adjustRange();

		cchart.legend = new ChartLegend(comp);
		return cchart;
	}

	public void setLabel(ILabelProvider label) {
		legend.label = label;
	}

}
