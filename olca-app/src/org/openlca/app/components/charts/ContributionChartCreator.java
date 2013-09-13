package org.openlca.app.components.charts;

import java.util.Collections;
import java.util.List;

import org.eclipse.birt.chart.model.Chart;
import org.eclipse.birt.chart.model.ChartWithoutAxes;
import org.eclipse.birt.chart.model.attribute.ChartDimension;
import org.eclipse.birt.chart.model.attribute.ChartType;
import org.eclipse.birt.chart.model.attribute.ColorDefinition;
import org.eclipse.birt.chart.model.attribute.Fill;
import org.eclipse.birt.chart.model.component.Series;
import org.eclipse.birt.chart.model.component.impl.SeriesImpl;
import org.eclipse.birt.chart.model.data.NumberDataSet;
import org.eclipse.birt.chart.model.data.SeriesDefinition;
import org.eclipse.birt.chart.model.data.TextDataSet;
import org.eclipse.birt.chart.model.data.impl.NumberDataSetImpl;
import org.eclipse.birt.chart.model.data.impl.SeriesDefinitionImpl;
import org.eclipse.birt.chart.model.data.impl.TextDataSetImpl;
import org.eclipse.birt.chart.model.impl.ChartWithoutAxesImpl;
import org.eclipse.birt.chart.model.type.PieSeries;
import org.eclipse.birt.chart.model.type.impl.PieSeriesImpl;
import org.eclipse.emf.common.util.EList;

/**
 * Creates a simple pie-chart for a set of numbers. No title or legend entries
 * are set. The color of the last entry is set to gray, for each other entry
 * FaviColor[i] is taken.
 */
class ContributionChartCreator {

	private List<Double> data;

	public ContributionChartCreator(List<Double> data) {
		if (data == null)
			this.data = Collections.emptyList();
		else
			this.data = data;
	}

	public Chart createChart(boolean withRest) {
		ChartWithoutAxes chart = ChartWithoutAxesImpl.create();
		chart.setDimension(ChartDimension.TWO_DIMENSIONAL_LITERAL);
		chart.setType(ChartType.PIE_LITERAL.getLiteral());
		chart.getTitle().getLabel().getCaption().setValue(null);
		chart.getTitle().setVisible(false);
		chart.getLegend().setVisible(false);
		SeriesDefinition categoryDefinition = createCategorySeries(chart);
		addColors(categoryDefinition, withRest);
		addNumberSeries(categoryDefinition);
		return chart;
	}

	private SeriesDefinition createCategorySeries(ChartWithoutAxes chart) {
		String[] categories = new String[data.size()];
		for (int i = 0; i < categories.length; i++)
			categories[i] = "value " + i;
		TextDataSet categoryData = TextDataSetImpl.create(categories);
		SeriesDefinition categoryDefinition = SeriesDefinitionImpl.create();
		chart.getSeriesDefinitions().add(categoryDefinition);
		Series categorySeries = SeriesImpl.create();
		categorySeries.setDataSet(categoryData);
		categoryDefinition.getSeries().add(categorySeries);
		return categoryDefinition;
	}

	private void addColors(SeriesDefinition categoryDefinition, boolean withRest) {
		EList<Fill> pallette = categoryDefinition.getSeriesPalette()
				.getEntries();
		pallette.clear();
		if (data.size() == 0)
			return;
		for (int i = 0; i < data.size() - 1; i++) {
			ColorDefinition color = ChartFaviColor.getColor(i);
			pallette.add(color);
		}
		if (withRest)
			pallette.add(ChartFaviColor.getGray());
		else
			pallette.add(ChartFaviColor.getColor(data.size() - 1));
	}

	private void addNumberSeries(SeriesDefinition categoryDefinition) {
		NumberDataSet values = NumberDataSetImpl.create(data);
		PieSeries pieSeries = (PieSeries) PieSeriesImpl.create();
		pieSeries.setDataSet(values);
		// pieSeries.setLabelPosition(Position.INSIDE_LITERAL);
		pieSeries.getLabel().setVisible(false);
		pieSeries.setExplosion(1);
		pieSeries.setRotation(90);
		pieSeries.getDataPoint().getComponents().clear();
		SeriesDefinition pieDefinition = SeriesDefinitionImpl.create();
		pieDefinition.getSeries().add(pieSeries);
		categoryDefinition.getSeriesDefinitions().add(pieDefinition);
	}

}
