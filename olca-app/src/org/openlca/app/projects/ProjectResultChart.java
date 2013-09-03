package org.openlca.app.projects;

import java.util.List;

import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.attribute.ChartDimension;
import org.eclipse.birt.chart.model.attribute.IntersectionType;
import org.eclipse.birt.chart.model.attribute.LegendItemType;
import org.eclipse.birt.chart.model.attribute.impl.ColorDefinitionImpl;
import org.eclipse.birt.chart.model.component.Axis;
import org.eclipse.birt.chart.model.component.Series;
import org.eclipse.birt.chart.model.component.impl.SeriesImpl;
import org.eclipse.birt.chart.model.data.NumberDataSet;
import org.eclipse.birt.chart.model.data.SeriesDefinition;
import org.eclipse.birt.chart.model.data.TextDataSet;
import org.eclipse.birt.chart.model.data.impl.NumberDataSetImpl;
import org.eclipse.birt.chart.model.data.impl.SeriesDefinitionImpl;
import org.eclipse.birt.chart.model.data.impl.TextDataSetImpl;
import org.eclipse.birt.chart.model.impl.ChartWithAxesImpl;
import org.eclipse.birt.chart.model.type.BarSeries;
import org.eclipse.birt.chart.model.type.impl.BarSeriesImpl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.FaviColor;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.util.Labels;
import org.openlca.core.editors.ChartViewer;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ContributionSet;

/**
 * A chart that displays the flow or impact results of a set of project
 * variants.
 */
public class ProjectResultChart extends Composite {

	private ChartViewer viewer;

	public ProjectResultChart(Composite parent) {
		super(parent, SWT.NONE);
		setLayout(new GridLayout());
		viewer = new ChartViewer(this);
		viewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.addPaintListener(viewer);
	}

	public void renderChart(BaseDescriptor reference,
			ContributionSet<ProjectVariant> set) {
		List<Contribution<ProjectVariant>> contributions = set
				.getContributions();
		ChartWithAxes chart = ChartWithAxesImpl.create();
		chart.setDimension(ChartDimension.TWO_DIMENSIONAL_LITERAL);
		chart.getTitle().setVisible(false);
		chart.getLegend().setVisible(true);
		chart.getLegend().setItemType(LegendItemType.CATEGORIES_LITERAL);
		Axis xAxis = xAxis(chart);
		Axis yAxis = yAxis(chart, xAxis, reference);
		createCategorySeries(xAxis, contributions);
		createBarSeries(yAxis, contributions);
		viewer.updateChart(chart);
	}

	private Axis yAxis(ChartWithAxes chart, Axis xAxis, BaseDescriptor reference) {
		String unit = "unit?";
		if (reference instanceof FlowDescriptor) {
			FlowDescriptor flow = (FlowDescriptor) reference;
			unit = Labels.getRefUnit(flow, Database.getCache());
		}
		Axis yAxis = chart.getPrimaryOrthogonalAxis(xAxis);
		yAxis.getTitle().getCaption().setValue("Results [" + unit + "]");
		yAxis.getLabel().getCaption().getFont().setSize(10);
		yAxis.getTitle().setVisible(true);
		return yAxis;
	}

	private Axis xAxis(ChartWithAxes chart) {
		Axis xAxis = chart.getPrimaryBaseAxes()[0];
		xAxis.getOrigin().setType(IntersectionType.VALUE_LITERAL);
		xAxis.getLabel().setVisible(false);
		return xAxis;
	}

	private void createCategorySeries(Axis xAxis,
			List<Contribution<ProjectVariant>> contributions) {
		String[] variantNames = new String[contributions.size()];
		for (int i = 0; i < contributions.size(); i++)
			variantNames[i] = contributions.get(i).getItem().getName();
		TextDataSet xAxisValues = TextDataSetImpl.create(variantNames);
		Series series = SeriesImpl.create();
		series.setDataSet(xAxisValues);
		SeriesDefinition seriesDefinitionX = SeriesDefinitionImpl.create();
		addFaviColors(contributions, seriesDefinitionX);
		xAxis.getSeriesDefinitions().add(seriesDefinitionX);
		seriesDefinitionX.getSeries().add(series);
	}

	private void addFaviColors(
			List<Contribution<ProjectVariant>> contributions,
			SeriesDefinition seriesDefinition) {
		seriesDefinition.getSeriesPalette().getEntries().clear();
		for (int i = 0; i < contributions.size(); i++) {
			Color c = FaviColor.getForChart(i);
			seriesDefinition
					.getSeriesPalette()
					.getEntries()
					.add(ColorDefinitionImpl.create(c.getRed(), c.getGreen(),
							c.getBlue()));
		}
	}

	private void createBarSeries(Axis yAxis,
			List<Contribution<ProjectVariant>> contributions) {
		double[] values = new double[contributions.size()];
		for (int i = 0; i < contributions.size(); i++)
			values[i] = contributions.get(i).getAmount();
		NumberDataSet dataSet = NumberDataSetImpl.create(values);
		BarSeries barSeries = (BarSeries) BarSeriesImpl.create();
		barSeries.setDataSet(dataSet);
		barSeries.setSeriesIdentifier(Messages.ImpactCategories);
		SeriesDefinition definition = SeriesDefinitionImpl.create();
		definition.getSeries().add(barSeries);
		yAxis.getSeriesDefinitions().add(definition);
	}
}
