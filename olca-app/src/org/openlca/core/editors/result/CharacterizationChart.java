/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.result;

import java.util.List;

import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.attribute.ChartDimension;
import org.eclipse.birt.chart.model.attribute.HorizontalAlignment;
import org.eclipse.birt.chart.model.attribute.IntersectionType;
import org.eclipse.birt.chart.model.attribute.LegendItemType;
import org.eclipse.birt.chart.model.attribute.Position;
import org.eclipse.birt.chart.model.attribute.TextAlignment;
import org.eclipse.birt.chart.model.attribute.VerticalAlignment;
import org.eclipse.birt.chart.model.attribute.impl.ColorDefinitionImpl;
import org.eclipse.birt.chart.model.attribute.impl.FontDefinitionImpl;
import org.eclipse.birt.chart.model.attribute.impl.TextAlignmentImpl;
import org.eclipse.birt.chart.model.component.Axis;
import org.eclipse.birt.chart.model.component.Series;
import org.eclipse.birt.chart.model.component.impl.SeriesImpl;
import org.eclipse.birt.chart.model.data.NumberDataSet;
import org.eclipse.birt.chart.model.data.SeriesDefinition;
import org.eclipse.birt.chart.model.data.TextDataSet;
import org.eclipse.birt.chart.model.data.impl.NumberDataElementImpl;
import org.eclipse.birt.chart.model.data.impl.NumberDataSetImpl;
import org.eclipse.birt.chart.model.data.impl.SeriesDefinitionImpl;
import org.eclipse.birt.chart.model.data.impl.TextDataSetImpl;
import org.eclipse.birt.chart.model.impl.ChartWithAxesImpl;
import org.eclipse.birt.chart.model.type.BarSeries;
import org.eclipse.birt.chart.model.type.impl.BarSeriesImpl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.core.editors.ChartViewer;
import org.openlca.core.model.results.LCIACategoryResult;

/**
 * The chart composite for displaying the characterization as a chart
 * 
 * @author Sebastian Greve
 * 
 */
public class CharacterizationChart extends Composite {

	private ChartViewer viewer;

	public CharacterizationChart(Composite parent,
			List<LCIACategoryResult> characterizations, int type,
			String weightingUnit) {
		super(parent, SWT.NONE);
		setLayout(new GridLayout());
		viewer = new ChartViewer(this);
		viewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.addPaintListener(viewer);
		createChart(characterizations, type, weightingUnit);
	}

	/**
	 * Creates a new chart
	 * 
	 * @param characterizations
	 *            The LCIA category results to display
	 * @param type
	 *            The type of chart
	 * @param weightingUnit
	 *            The weighting unit
	 */
	public void createChart(List<LCIACategoryResult> characterizations,
			int type, String weightingUnit) {
		// create chart
		final ChartWithAxes chart = ChartWithAxesImpl.create();
		// set title
		chart.getTitle()
				.getLabel()
				.getCaption()
				.setValue(
						type == CharacterizationPage.NORMALIZATION ? Messages.NormalizedResults
								: Messages.WeightedResults);
		chart.setDimension(ChartDimension.TWO_DIMENSIONAL_WITH_DEPTH_LITERAL);

		// configure legened
		chart.getLegend().setVisible(true);
		chart.getLegend().setItemType(LegendItemType.CATEGORIES_LITERAL);

		// get and configure x-axis
		final Axis xAxis = chart.getPrimaryBaseAxes()[0];
		xAxis.getTitle().getCaption()
				.setValue(Messages.ImpactCategories);
		xAxis.getTitle().setVisible(false);
		xAxis.getLabel().setVisible(false);
		final TextAlignment xLabelAlignment = TextAlignmentImpl.create();
		xLabelAlignment
				.setHorizontalAlignment(HorizontalAlignment.CENTER_LITERAL);
		xLabelAlignment.setVerticalAlignment(VerticalAlignment.CENTER_LITERAL);
		xAxis.getLabel()
				.getCaption()
				.setFont(
						FontDefinitionImpl.create(xAxis.getLabel().getCaption()
								.getFont().getName(), 12, false, false, false,
								false, false, -45, xLabelAlignment));
		xAxis.getOrigin().setType(IntersectionType.VALUE_LITERAL);
		xAxis.getOrigin().setValue(NumberDataElementImpl.create(0));

		// get and configure y-axis
		final Axis yAxis = chart.getPrimaryOrthogonalAxis(xAxis);
		yAxis.getTitle().getCaption()
				.setValue(weightingUnit != null ? weightingUnit : ""); 
		yAxis.getTitle().setVisible(true);

		// get category names as string array
		final String[] categoryNames = new String[characterizations.size()];
		for (int i = 0; i < characterizations.size(); i++) {
			LCIACategoryResult characterization = characterizations.get(i);
			categoryNames[i] = characterization.getCategory();
		}

		// create x-axis series
		final TextDataSet xAxisValues = TextDataSetImpl.create(categoryNames);
		final Series seCategory = SeriesImpl.create();
		seCategory.setDataSet(xAxisValues);
		final SeriesDefinition seriesDefinitionX = SeriesDefinitionImpl
				.create();
		seriesDefinitionX.getSeriesPalette().getEntries().clear();
		for (int i = 0; i < characterizations.size(); i++) {
			seriesDefinitionX
					.getSeriesPalette()
					.getEntries()
					.add(ColorDefinitionImpl.create(
							(int) (Math.random() * 255),
							(int) (Math.random() * 255),
							(int) (Math.random() * 255)));
		}
		xAxis.getSeriesDefinitions().add(seriesDefinitionX);
		seriesDefinitionX.getSeries().add(seCategory);

		// get values
		final double[] values = new double[characterizations.size()];
		for (int i = 0; i < characterizations.size(); i++) {
			final LCIACategoryResult characterization = characterizations
					.get(i);
			if (type == CharacterizationPage.NORMALIZATION) {
				values[i] = characterization.getNormalizedValue();
			} else if (type == CharacterizationPage.WEIGHTING) {
				values[i] = characterization.getWeightedValue();
			}
		}

		// create y-axis series
		final NumberDataSet yAxisValues = NumberDataSetImpl.create(values);
		final BarSeries barSeries = (BarSeries) BarSeriesImpl.create();
		barSeries.setDataSet(yAxisValues);
		barSeries.getLabel().setVisible(true);
		barSeries.getLabel().setVisible(true);
		barSeries.setSeriesIdentifier(Messages.ImpactCategories);
		barSeries.setLabelPosition(Position.INSIDE_LITERAL);
		final SeriesDefinition seriesDefinitionY = SeriesDefinitionImpl
				.create();
		seriesDefinitionY.getSeries().add(barSeries);
		yAxis.getSeriesDefinitions().add(seriesDefinitionY);

		// update viewer
		viewer.updateChart(chart);
	}

}
