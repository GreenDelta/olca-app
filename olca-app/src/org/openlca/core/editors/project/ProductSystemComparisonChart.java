/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.birt.chart.model.Chart;
import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.attribute.ChartDimension;
import org.eclipse.birt.chart.model.attribute.ColorDefinition;
import org.eclipse.birt.chart.model.attribute.Fill;
import org.eclipse.birt.chart.model.attribute.HorizontalAlignment;
import org.eclipse.birt.chart.model.attribute.IntersectionType;
import org.eclipse.birt.chart.model.attribute.LegendItemType;
import org.eclipse.birt.chart.model.attribute.Position;
import org.eclipse.birt.chart.model.attribute.RiserType;
import org.eclipse.birt.chart.model.attribute.TextAlignment;
import org.eclipse.birt.chart.model.attribute.VerticalAlignment;
import org.eclipse.birt.chart.model.attribute.impl.ColorDefinitionImpl;
import org.eclipse.birt.chart.model.attribute.impl.FontDefinitionImpl;
import org.eclipse.birt.chart.model.attribute.impl.GradientImpl;
import org.eclipse.birt.chart.model.attribute.impl.JavaNumberFormatSpecifierImpl;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.editors.ChartViewer;
import org.openlca.core.math.ImpactCalculator;
import org.openlca.core.math.MatrixSolver;
import org.openlca.core.model.LCIACategory;
import org.openlca.core.model.LCIAMethod;
import org.openlca.core.model.NormalizationWeightingSet;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.core.model.results.LCIACategoryResult;
import org.openlca.core.model.results.LCIAResult;
import org.openlca.core.model.results.LCIResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The chart composite for comparing product systems of a project
 * 
 * @author Sebastian Greve
 * 
 */
public class ProductSystemComparisonChart extends Composite {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private Chart chart;
	private IDatabase database;
	private RGB[] lciaColors = new RGB[0];
	private Project project;
	private RGB[] psColors = new RGB[0];
	private Map<String, LCIResult> resultMap = new HashMap<>();
	private ChartViewer viewer;

	public ProductSystemComparisonChart(Composite parent, Project project,
			IDatabase database) {
		super(parent, SWT.NONE);
		this.database = database;
		setLayout(new GridLayout());
		this.project = project;
		viewer = new ChartViewer(this);
		viewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.addPaintListener(viewer);
		refreshColors(0);
	}

	private Chart createNormalChart(String method, String referenceSystem,
			String[] categoryNames, List<Double[]> values,
			boolean showValuesOnBarSeries, String unit) {
		ChartWithAxes chart = ChartWithAxesImpl.create();
		String title = NLS.bind(
				Messages.CompareProductSystemsForMethod, method
						+ (referenceSystem != null ? " - " + referenceSystem
								: ""));
		chart.getTitle().getLabel().getCaption().setValue(title);
		chart.setDimension(ChartDimension.TWO_DIMENSIONAL_WITH_DEPTH_LITERAL);

		chart.getLegend().setItemType(LegendItemType.SERIES_LITERAL);
		chart.getLegend().getClientArea().getOutline().setVisible(true);
		chart.getLegend().getTitle().setVisible(false);

		Axis xAxis = chart.getPrimaryBaseAxes()[0];
		xAxis.getTitle().getCaption()
				.setValue(Messages.ImpactCategories);
		TextAlignment xLabelAlignment = TextAlignmentImpl.create();
		xLabelAlignment
				.setHorizontalAlignment(HorizontalAlignment.CENTER_LITERAL);
		xLabelAlignment.setVerticalAlignment(VerticalAlignment.CENTER_LITERAL);
		xAxis.getLabel()
				.getCaption()
				.setFont(
						FontDefinitionImpl.create("Arial", 12, false, false,
								false, false, false, -45, xLabelAlignment));

		xAxis.getOrigin().setType(IntersectionType.VALUE_LITERAL);
		xAxis.getOrigin().setValue(NumberDataElementImpl.create(0));

		Axis yAxis = chart.getPrimaryOrthogonalAxis(xAxis);
		yAxis.getTitle().getCaption().setValue(unit);
		yAxis.getTitle().setVisible(true);
		if (referenceSystem == null) {
			yAxis.setPercent(true);
			yAxis.getScale().setMax(NumberDataElementImpl.create(100));
			for (int psCount = 0; psCount < project.getProductSystems().length; psCount++) {
				boolean b = false;
				for (int i = 0; i < values.get(psCount).length; i++) {
					if (values.get(psCount)[i] < 0) {
						yAxis.getScale().setMin(
								NumberDataElementImpl.create(-100));
						b = true;
						break;
					}
				}
				if (b) {
					break;
				}
			}
		}

		TextDataSet xAxisValues = TextDataSetImpl.create(categoryNames);

		Series seCategory = SeriesImpl.create();
		seCategory.setDataSet(xAxisValues);

		SeriesDefinition seriesDefinitionX = SeriesDefinitionImpl.create();

		seriesDefinitionX.getSeries().add(seCategory);
		xAxis.getSeriesDefinitions().add(seriesDefinitionX);

		for (int psCount = 0; psCount < project.getProductSystems().length; psCount++) {
			// create bar series for each product system
			NumberDataSet yAxisValues = NumberDataSetImpl.create(values
					.get(psCount));
			BarSeries barSeries = (BarSeries) BarSeriesImpl.create();
			barSeries.setDataSet(yAxisValues);
			barSeries.getLabel().setVisible(showValuesOnBarSeries);
			TextAlignment alignment = TextAlignmentImpl.create();
			alignment
					.setHorizontalAlignment(HorizontalAlignment.CENTER_LITERAL);
			alignment.setVerticalAlignment(VerticalAlignment.CENTER_LITERAL);
			barSeries
					.getLabel()
					.getCaption()
					.setFont(
							FontDefinitionImpl.create(barSeries.getLabel()
									.getCaption().getFont().getName(), 12,
									true, false, false, false, false, 90,
									alignment));

			try {
				IModelComponent descriptor = database.selectDescriptor(
						ProductSystem.class,
						project.getProductSystems()[psCount]);
				barSeries.setSeriesIdentifier(descriptor.getName());
			} catch (Exception e) {
				log.error("Reading descriptor from db failed", e);
			}
			barSeries.setLabelPosition(Position.INSIDE_LITERAL);
			SeriesDefinition seriesDefinitionY = SeriesDefinitionImpl.create();
			// create the colors
			Fill[] fiaBase = new Fill[values.get(psCount).length];
			for (int j = 0; j < fiaBase.length; j++) {
				ColorDefinition color1 = ColorDefinitionImpl.create(
						psColors[psCount].red, psColors[psCount].green,
						psColors[psCount].blue);
				color1.setTransparency(200);
				ColorDefinition color2 = ColorDefinitionImpl.create(
						psColors[psCount].red + 35,
						psColors[psCount].green + 35,
						psColors[psCount].blue + 35);
				color2.setTransparency(200);
				fiaBase[j] = GradientImpl.create(color1, color2);
			}
			seriesDefinitionY.getSeriesPalette().getEntries().clear();
			for (Fill element : fiaBase) {
				seriesDefinitionY.getSeriesPalette().getEntries().add(element);
			}
			seriesDefinitionY.getSeries().add(barSeries);
			seriesDefinitionY.setFormatSpecifier(JavaNumberFormatSpecifierImpl
					.create("#.0###E0"));
			yAxis.getSeriesDefinitions().add(seriesDefinitionY);
		}
		return chart;
	}

	/**
	 * Creates a new single score chart (stacked chart)
	 * 
	 * @param method
	 *            The name of the selected LCIA method
	 * @param referenceSystem
	 *            The name of the selected normalization and weighting set
	 * @param categoryNames
	 *            The LCIA category names
	 * @param values
	 *            The values for each category
	 * @param showValuesOnBarSeries
	 *            Indicates if the double values should be shown in the chart
	 * @param unit
	 *            The unit to display
	 * @return The created chart
	 */
	private Chart createSingleScoreChart(String method, String referenceSystem,
			String[] categoryNames, List<Double[]> values,
			boolean showValuesOnBarSeries, String unit) {
		ChartWithAxes chart = ChartWithAxesImpl.create();
		chart.getTitle()
				.getLabel()
				.getCaption()
				.setValue(
						NLS.bind(Messages.SingleScoreTitle, method
								+ " - " + referenceSystem));
		chart.setDimension(ChartDimension.TWO_DIMENSIONAL_WITH_DEPTH_LITERAL);

		Legend legend = chart.getLegend();
		legend.setVisible(true);

		Axis xAxis = chart.getPrimaryBaseAxes()[0];
		xAxis.getTitle().setVisible(false);
		TextAlignment xLabelAlignment = TextAlignmentImpl.create();
		xLabelAlignment
				.setHorizontalAlignment(HorizontalAlignment.CENTER_LITERAL);
		xLabelAlignment.setVerticalAlignment(VerticalAlignment.CENTER_LITERAL);
		xAxis.getLabel()
				.getCaption()
				.setFont(
						FontDefinitionImpl.create("Arial", 12, false, false,
								false, false, false, -45, xLabelAlignment));

		Axis yAxis = chart.getPrimaryOrthogonalAxis(xAxis);
		yAxis.getTitle().getCaption().setValue(unit);
		yAxis.getTitle().setVisible(true);

		xAxis.getOrigin().setType(IntersectionType.VALUE_LITERAL);
		xAxis.getOrigin().setValue(NumberDataElementImpl.create(0));

		String[] psNames = new String[project.getProductSystems().length];
		for (int i = 0; i < project.getProductSystems().length; i++) {
			try {
				psNames[i] = database.selectDescriptor(ProductSystem.class,
						project.getProductSystems()[i]).getName();
			} catch (Exception e) {
				log.error("Reading product system from db failed", e);
			}
		}
		TextDataSet psValues = TextDataSetImpl.create(psNames);

		List<NumberDataSet> dataSets = new ArrayList<>();

		for (int i = 0; i < categoryNames.length; i++) {
			double[] results = new double[values.size()];
			for (int j = 0; j < values.size(); j++) {
				results[j] = values.get(j)[i];
			}
			NumberDataSet seriesValues = NumberDataSetImpl.create(results);
			dataSets.add(seriesValues);
		}

		// X-Series
		Series seCategory = SeriesImpl.create();
		seCategory.setDataSet(psValues);

		SeriesDefinition sdX = SeriesDefinitionImpl.create();
		xAxis.getSeriesDefinitions().add(sdX);
		sdX.getSeries().add(seCategory);

		SeriesDefinition sdY1 = SeriesDefinitionImpl.create();
		Fill[] fiaBase = new Fill[categoryNames.length];
		for (int j = 0; j < fiaBase.length; j++) {
			ColorDefinition color1 = ColorDefinitionImpl.create(
					lciaColors[j].red, lciaColors[j].green, lciaColors[j].blue);
			color1.setTransparency(200);
			ColorDefinition color2 = ColorDefinitionImpl.create(
					lciaColors[j].red + 35, lciaColors[j].green + 35,
					lciaColors[j].blue + 35);
			color2.setTransparency(200);
			fiaBase[j] = GradientImpl.create(color1, color2);
		}
		sdY1.getSeriesPalette().getEntries().clear();
		for (Fill element : fiaBase) {
			sdY1.getSeriesPalette().getEntries().add(element);
		}
		yAxis.getSeriesDefinitions().add(sdY1);
		// Y-Series
		for (int i = 0; i < dataSets.size(); i++) {
			BarSeries bs1 = (BarSeries) BarSeriesImpl.create();
			bs1.setSeriesIdentifier(categoryNames[i]);
			bs1.setDataSet(dataSets.get(i));
			bs1.getLabel().setVisible(showValuesOnBarSeries);
			bs1.setRiserOutline(null);
			bs1.setRiser(RiserType.RECTANGLE_LITERAL);
			bs1.setStacked(true);
			TextAlignment alignment = TextAlignmentImpl.create();
			alignment
					.setHorizontalAlignment(HorizontalAlignment.CENTER_LITERAL);
			alignment.setVerticalAlignment(VerticalAlignment.CENTER_LITERAL);
			bs1.getLabel()
					.getCaption()
					.setFont(
							FontDefinitionImpl.create(bs1.getLabel()
									.getCaption().getFont().getName(), 12,
									true, false, false, false, false, 0,
									alignment));
			bs1.setLabelPosition(Position.INSIDE_LITERAL);
			sdY1.getSeries().add(bs1);
		}
		sdY1.setFormatSpecifier(JavaNumberFormatSpecifierImpl
				.create("#.0###E0"));
		return chart;
	}

	/**
	 * Looks up each LCIA category results for one product system for the
	 * maximum value
	 * 
	 * @param type
	 *            The type of chart to be drawn
	 * @param characterizationMap
	 *            Map containing the LCIA category results to look up
	 * @return The maximum value of each LCIA category results for one product
	 *         system
	 */
	private double[] getMaximumValues(int type,
			Map<String, LCIACategoryResult[]> characterizationMap) {
		double[] maximums = null;
		try {
			for (String id : project.getProductSystems()) {
				LCIACategoryResult[] characterizations = characterizationMap
						.get(id);
				if (characterizations != null) {
					for (int j = 0; j < characterizations.length; j++) {
						LCIACategoryResult actualResult = characterizations[j];
						double value = 0;
						switch (type) {
						case ProjectComparisonPage.CHARACTERIZATION:
							value = actualResult.getValue();
							break;
						case ProjectComparisonPage.NORMALIZATION:
							value = actualResult.getNormalizedValue();
							break;
						case ProjectComparisonPage.WEIGHTING:
							value = actualResult.getWeightedValue();
							break;
						}
						if (maximums == null) {
							maximums = new double[characterizations.length];
						}
						if (maximums[j] < Math.abs(value)) {
							maximums[j] = Math.abs(value);
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("Get maximum values failed", e);
		}
		return maximums != null ? maximums : new double[0];
	}

	/**
	 * Loads the LCI results of each product system of the project
	 */
	private void loadResults() {
		for (String id : project.getProductSystems()) {
			if (resultMap.get(id) == null) {
				try {
					LCIResult result = database.select(LCIResult.class, id);
					resultMap.put(id, result);
				} catch (Exception e) {
					log.error("Reading LCI result from db failed", e);
				}
			}
		}
	}

	/**
	 * Creates new colors if the amount of categories to be drawn has been
	 * extended
	 * 
	 * @param categories
	 *            The amount of categories to be drawn
	 */
	private void refreshColors(int categories) {
		if (psColors.length < project.getProductSystems().length) {
			// add new colors for added product systems after first
			// initialization
			RGB[] old = psColors;
			psColors = new RGB[project.getProductSystems().length];
			for (int c = 0; c < old.length; c++) {
				psColors[c] = new RGB(old[c].red, old[c].green, old[c].blue);
			}
			for (int c = old.length; c < project.getProductSystems().length; c++) {
				int red = (int) (Math.random() * 220);
				int green = (int) (Math.random() * 220);
				int blue = (int) (Math.random() * 220);
				psColors[c] = new RGB(red, green, blue);
			}
		}

		if (lciaColors.length < categories) {
			// add new colors categories
			RGB[] old = lciaColors;
			lciaColors = new RGB[categories];
			for (int c = 0; c < old.length; c++) {
				lciaColors[c] = new RGB(old[c].red, old[c].green, old[c].blue);
			}
			for (int c = old.length; c < categories; c++) {
				int red = (int) (Math.random() * 220);
				int green = (int) (Math.random() * 220);
				int blue = (int) (Math.random() * 220);
				lciaColors[c] = new RGB(red, green, blue);
			}
		}
	}

	/**
	 * Creates the chart
	 * 
	 * @param method
	 *            the LCIA method
	 * @param normalizationWeightingSet
	 *            The selected normalization and weighting set
	 * @param categories
	 *            the categories selected to be drawn
	 * @param showValuesOnBarSeries
	 *            if true, show results on bar series
	 * @param type
	 *            The type of chart to be drawn
	 * @return the chart
	 */
	public Chart createChart(LCIAMethod method,
			NormalizationWeightingSet normalizationWeightingSet,
			LCIACategory[] categories, boolean showValuesOnBarSeries, int type) {
		Chart chart = null;

		refreshColors(categories.length);

		String[] categoryNames = new String[categories.length];
		int i = 0;
		for (LCIACategory category : categories) {
			categoryNames[i] = category.getName();
			i++;
		}

		loadResults();

		List<Double[]> values = new ArrayList<>();
		Map<String, LCIACategoryResult[]> characterizationMap = new HashMap<>();
		// calculate results
		for (String id : project.getProductSystems()) {
			LCIResult result = resultMap.get(id);
			if (result == null)
				result = calculate(id);
			ImpactCalculator calculator = new ImpactCalculator(database, result);
			ImpactMethodDescriptor methodDes = toDescriptor(method);

			LCIAResult impactResult = calculator.calculate(methodDes,
					normalizationWeightingSet);
			if (impactResult != null) {
				List<LCIACategoryResult> results = impactResult
						.getLCIACategoryResults();
				characterizationMap
						.put(id, results.toArray(new LCIACategoryResult[results
								.size()]));
			}
		}

		double[] maximums = null;
		if (type == ProjectComparisonPage.CHARACTERIZATION) {
			maximums = getMaximumValues(type, characterizationMap);
		}
		for (String id : project.getProductSystems()) {
			Double[] actualValues = new Double[categories.length];
			LCIACategoryResult[] characterizations = characterizationMap
					.get(id);
			for (int j = 0; j < categories.length; j++) {
				LCIACategoryResult actualResult = characterizations[j];

				double value = 0;
				switch (type) {
				case ProjectComparisonPage.CHARACTERIZATION:
					value = actualResult.getValue();
					break;
				case ProjectComparisonPage.NORMALIZATION:
					value = actualResult.getNormalizedValue();
					break;
				case ProjectComparisonPage.WEIGHTING:
					value = actualResult.getWeightedValue();
					break;
				case ProjectComparisonPage.SINGLE_SCORE:
					value = actualResult.getWeightedValue();
					break;
				}
				if (type == ProjectComparisonPage.CHARACTERIZATION
						&& maximums != null) {
					// show as percentage value
					actualValues[j] = value / maximums[j] * 100;
				} else {
					actualValues[j] = value;
				}
				if (actualValues[j] >= 1) {
					actualValues[j] = (double) (int) (actualValues[j] * 10000) / 10000;
				}
			}
			values.add(actualValues);
		}

		String referenceSystem = normalizationWeightingSet != null ? normalizationWeightingSet
				.getReferenceSystem() : "";
		String unit = null;
		if (type == ProjectComparisonPage.SINGLE_SCORE) {
			unit = normalizationWeightingSet != null ? normalizationWeightingSet
					.getUnit() : "";
			chart = createSingleScoreChart(method.getName(), referenceSystem,
					categoryNames, values, showValuesOnBarSeries, unit);
		} else {
			switch (type) {
			case ProjectComparisonPage.CHARACTERIZATION:
				unit = "%";
				break;
			case ProjectComparisonPage.NORMALIZATION:
				unit = "";
				break;
			case ProjectComparisonPage.WEIGHTING:
				if (normalizationWeightingSet != null) {
					unit = normalizationWeightingSet.getUnit();
				}
				break;
			}
			chart = createNormalChart(
					method.getName(),
					type != ProjectComparisonPage.CHARACTERIZATION ? normalizationWeightingSet != null ? normalizationWeightingSet
							.getReferenceSystem() : null
							: null, categoryNames, values,
					showValuesOnBarSeries, unit);
		}
		return chart;
	}

	private LCIResult calculate(String systemId) {
		try {
			ProductSystem system = database.select(ProductSystem.class,
					systemId);
			MatrixSolver solver = new MatrixSolver(database);
			LCIResult result = solver.calculate(system);
			return result;
		} catch (Exception e) {
			log.error("Failed to calculation of system failed " + systemId, e);
			LCIResult empty = new LCIResult();
			empty.setProductName(systemId);
			empty.setProductSystemId(systemId);
			return empty;
		}
	}

	public void setData(LCIAMethod method,
			NormalizationWeightingSet normalizationWeightingSet,
			LCIACategory[] categories, boolean showValuesOnBarSeries, int type) {
		chart = createChart(method, normalizationWeightingSet, categories,
				showValuesOnBarSeries, type);
		viewer.updateChart(chart);
	}

	/*
	 * TODO: this is only a temporary method. the LCIA methods in the project
	 * editor should be replaced by method descriptors.
	 */
	private ImpactMethodDescriptor toDescriptor(LCIAMethod method) {
		ImpactMethodDescriptor d = new ImpactMethodDescriptor();
		d.setDescription(method.getDescription());
		d.setId(method.getId());
		d.setName(method.getName());
		for (LCIACategory cat : method.getLCIACategories()) {
			ImpactCategoryDescriptor catDes = new ImpactCategoryDescriptor();
			catDes.setDescription(cat.getDescription());
			catDes.setId(cat.getId());
			catDes.setName(cat.getName());
			catDes.setReferenceUnit(cat.getReferenceUnit());
			d.getImpactCategories().add(catDes);
		}
		return d;
	}

}
