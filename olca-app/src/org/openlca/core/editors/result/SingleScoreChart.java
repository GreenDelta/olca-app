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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.attribute.ChartDimension;
import org.eclipse.birt.chart.model.attribute.HorizontalAlignment;
import org.eclipse.birt.chart.model.attribute.IntersectionType;
import org.eclipse.birt.chart.model.attribute.Position;
import org.eclipse.birt.chart.model.attribute.RiserType;
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
import org.eclipse.birt.chart.model.layout.Legend;
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
public class SingleScoreChart extends Composite {

	private ChartViewer viewer;

	public SingleScoreChart(Composite parent,
			List<LCIACategoryResult> characterizations, String weightingUnit) {
		super(parent, SWT.NONE);
		setLayout(new GridLayout());
		viewer = new ChartViewer(this);
		viewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.addPaintListener(viewer);
		createChart(characterizations, weightingUnit);
	}

	/**
	 * Creates a new single score chart
	 * 
	 * @param characterizations
	 *            The LCIA category result to display
	 * @param weightingUnit
	 *            The weighting unit
	 */
	public void createChart(List<LCIACategoryResult> characterizations,
			String weightingUnit) {
		// create chart
		ChartWithAxes chart = ChartWithAxesImpl.create();
		// set title
		chart.getTitle().getLabel().getCaption()
				.setValue(Messages.SingleScoreResult);
		chart.setDimension(ChartDimension.TWO_DIMENSIONAL_WITH_DEPTH_LITERAL);

		// configure legend
		final Legend legend = chart.getLegend();
		legend.setVisible(true);

		// get and configure x-axis
		final Axis xAxis = chart.getPrimaryBaseAxes()[0];
		xAxis.getTitle().setVisible(false);
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

		// Data Set
		final String[] saTextValues = { "" };
		final TextDataSet categoryValues = TextDataSetImpl.create(saTextValues);
		final List<NumberDataSet> values = new ArrayList<>();
		for (final LCIACategoryResult characterization : characterizations) {
			final NumberDataSet seriesValues = NumberDataSetImpl
					.create(new double[] { characterization.getWeightedValue() });
			values.add(seriesValues);
		}

		// Create x-series
		final Series seCategory = SeriesImpl.create();
		seCategory.setDataSet(categoryValues);
		final SeriesDefinition sdX = SeriesDefinitionImpl.create();
		xAxis.getSeriesDefinitions().add(sdX);
		sdX.getSeries().add(seCategory);

		// Create y-series
		final SeriesDefinition sdY1 = SeriesDefinitionImpl.create();
		sdY1.getSeriesPalette().getEntries().clear();
		for (int i = 0; i < characterizations.size(); i++) {
			sdY1.getSeriesPalette()
					.getEntries()
					.add(ColorDefinitionImpl.create(
							(int) (Math.random() * 255),
							(int) (Math.random() * 255),
							(int) (Math.random() * 255)));
		}

		yAxis.getSeriesDefinitions().add(sdY1);
		// Y-Series
		// create stacks
		for (int i = 0; i < characterizations.size(); i++) {
			final BarSeries bs1 = (BarSeries) BarSeriesImpl.create();
			bs1.setSeriesIdentifier(characterizations.get(i).getCategory());
			bs1.setDataSet(values.get(i));
			bs1.setLabelPosition(Position.INSIDE_LITERAL);
			bs1.getLabel().setVisible(true);
			bs1.setRiserOutline(null);
			bs1.setRiser(RiserType.RECTANGLE_LITERAL);
			bs1.setStacked(true);
			sdY1.getSeries().add(bs1);
		}

		viewer.updateChart(chart);

	}
}
