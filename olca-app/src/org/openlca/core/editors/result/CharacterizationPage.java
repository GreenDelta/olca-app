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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UIFactory;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.model.results.LCIACategoryResult;

/**
 * FormPage for displaying the lcia results
 * 
 * @author Sebastian Greve
 * 
 */
public class CharacterizationPage extends ModelEditorPage {

	/**
	 * Value for impact assessment analysis
	 */
	public static final int IMPACT_ASSESSMENT = 0;

	/**
	 * Value for normalization analysis
	 */
	public static final int NORMALIZATION = 1;

	/**
	 * Value for weighting analysis
	 */
	public static final int WEIGHTING = 2;

	/**
	 * Property of the lcia result table viewer
	 */
	private String AMOUNT = Messages.Common_Amount;

	/**
	 * Property of the lcia result table viewer
	 */
	private String CATEGORY = Messages.Results_LCIACategory;

	/**
	 * The characterization chart
	 */
	private CharacterizationChart chart;

	/**
	 * The sort direction
	 */
	private int direction = 1;

	/**
	 * The property of the last selected column
	 */
	private String lastColumnSelect = CATEGORY;

	/**
	 * The LCIA category results to display on the page
	 */
	private final List<LCIACategoryResult> lciaCategoryResults;

	/**
	 * The name of the product system
	 */
	private final String name;

	/**
	 * Properties of the lcia result table viewer
	 */
	private String[] PROPERTIES;

	/**
	 * Table viewer to display the results
	 */
	private TableViewer resultViewer;

	/**
	 * Property of the lcia result table viewer
	 */
	private String SD = Messages.Systems_SD;

	/**
	 * The single score chart
	 */
	private SingleScoreChart singleScoreChart;

	/**
	 * Stack layout
	 */
	private StackLayout stackLayout;

	/**
	 * The type of analysis
	 */
	private final int type;

	/**
	 * Property of the lcia result table viewer
	 */
	private String UNIT = Messages.Results_Unit;

	/**
	 * The weighting unit
	 */
	private final String weightingUnit;

	/**
	 * Public constructor
	 * 
	 * @param editor
	 *            The editor which holds this page
	 * @param title
	 *            The title of the page
	 * @param name
	 *            The name of the product system
	 * @param lciaCategoryResults
	 *            The LCIA category results to display on the page
	 * @param weightingUnit
	 *            The weighting unit
	 * @param id
	 *            The id of the page
	 * @param type
	 *            The type of analysis
	 */
	public CharacterizationPage(ModelEditor editor, String name,
			List<LCIACategoryResult> lciaCategoryResults, String weightingUnit,
			String title, String id, int type) {
		super(editor, id, title);
		this.name = name;
		this.type = type;
		this.lciaCategoryResults = lciaCategoryResults;
		switch (type) {
		case IMPACT_ASSESSMENT:
			PROPERTIES = new String[] { CATEGORY, AMOUNT, UNIT, SD };
			break;
		case NORMALIZATION:
			PROPERTIES = new String[] { CATEGORY, AMOUNT, SD };
			break;
		case WEIGHTING:
			PROPERTIES = new String[] { CATEGORY, AMOUNT, UNIT, SD };
			break;
		}
		this.weightingUnit = weightingUnit;
	}

	@Override
	protected void createContents(final Composite body,
			final FormToolkit toolkit) {

		resultViewer = UIFactory.createTableViewer(body, null, null, null,
				PROPERTIES, getDatabase());
		ImpactResultLabel labelProvider = new ImpactResultLabel(type);
		labelProvider.setWeightingUnit(weightingUnit); // null allowed
		resultViewer.setLabelProvider(labelProvider);

		resultViewer.getTable().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));

		resultViewer.setSorter(new LCIAResultViewerSorter(CATEGORY, 1));

		for (final TableColumn c : resultViewer.getTable().getColumns()) {
			if (c.getText().equals(CATEGORY)) {
				c.setWidth(150);
				c.getParent().setSortColumn(c);
				c.getParent().setSortDirection(SWT.DOWN);
			} else {
				c.pack();
			}
			c.setMoveable(true);
			c.setResizable(true);
		}

		final IToolBarManager toolBar = getForm().getToolBarManager();
		toolBar.removeAll();
		if (type == NORMALIZATION || type == WEIGHTING) {
			toolBar.add(new SwitchViewMode(SwitchViewMode.TABLE));
			toolBar.add(new SwitchViewMode(SwitchViewMode.CHART));
			if (type == WEIGHTING) {
				toolBar.add(new SwitchViewMode(SwitchViewMode.SINGLE_SCORE));
			}
			toolBar.update(true);
			chart = new CharacterizationChart(body, lciaCategoryResults, type,
					weightingUnit);
			if (type == WEIGHTING) {
				singleScoreChart = new SingleScoreChart(body,
						lciaCategoryResults, weightingUnit);
			}
		}
		stackLayout = new StackLayout();
		stackLayout.topControl = resultViewer.getTable();
		body.setLayout(stackLayout);

		body.layout();
	}

	@Override
	protected String getFormTitle() {
		String title = null;
		switch (type) {
		case NORMALIZATION:
			title = NLS.bind(Messages.Results_NormalizationOf, name);
			break;
		case IMPACT_ASSESSMENT:
			title = NLS.bind(Messages.Results_CharacterizationOf, name);
			break;
		case WEIGHTING:
			title = NLS.bind(Messages.Results_WeightingOf, name);
			break;
		}
		return title;
	}

	@Override
	protected void initListeners() {

		for (final TableColumn c : resultViewer.getTable().getColumns()) {
			c.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetDefaultSelected(final SelectionEvent e) {
					// no action on default selection
				}

				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (c.getText().equals(lastColumnSelect)) {
						direction = direction * -1;
					} else {
						direction = 1;
					}
					resultViewer.setSorter(new LCIAResultViewerSorter(c
							.getText(), direction));
					lastColumnSelect = c.getText();
					c.getParent().setSortColumn(c);
					c.getParent().setSortDirection(
							direction == 1 ? SWT.DOWN : SWT.UP);
				}
			});
		}

	}

	@Override
	protected void setData() {
		resultViewer.setInput(lciaCategoryResults);
	}

	@Override
	public void dispose() {
		super.dispose();
		CATEGORY = null;
		AMOUNT = null;
		UNIT = null;
		SD = null;
		PROPERTIES = null;
	}

	/**
	 * Sorter of the LCIA category results
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class LCIAResultViewerSorter extends ViewerSorter {

		/**
		 * The sort direction
		 */
		private int direction = 1;

		/**
		 * The property of the selected column to sort
		 */
		private final String property;

		/**
		 * Creates a new instance
		 * 
		 * @param property
		 *            The property of the selected column to sort
		 * @param direction
		 *            The sort direction
		 */
		public LCIAResultViewerSorter(final String property, final int direction) {
			this.property = property;
			this.direction = direction;
		}

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {
			final LCIACategoryResult result1 = (LCIACategoryResult) e1;
			final LCIACategoryResult result2 = (LCIACategoryResult) e2;
			int result = 0;
			if (property.equals(CATEGORY)) {
				// compare category names
				result = result1.getCategory().toLowerCase()
						.compareTo(result2.getCategory().toLowerCase());
			} else if (property.equals(AMOUNT)) {
				// compare amounts
				double value1 = 0;
				double value2 = 0;
				switch (type) {
				case IMPACT_ASSESSMENT:
					// compare values
					value1 = result1.getValue();
					value2 = result2.getValue();
					break;
				case NORMALIZATION:
					// compare normalized values
					value1 = result1.getNormalizedValue();
					value2 = result2.getNormalizedValue();
					break;
				case WEIGHTING:
					// compare weighted values
					value1 = result1.getWeightedValue();
					value2 = result2.getWeightedValue();
					break;
				}
				result = Double.compare(value1, value2);
			} else if (property.equals(UNIT)) {
				// compare unit
				if (type == WEIGHTING) {
					// all units are same
					result = 0;
				} else if (type == IMPACT_ASSESSMENT) {
					// compare unit names
					result = result1.getUnit().toLowerCase()
							.compareTo(result2.getUnit().toLowerCase());
				}
			} else if (property.equals(SD)) {
				// result = Double.compare(result1.getStandardDeviation(),
				// result2
				// .getStandardDeviation());
			}
			result = result * direction;
			return result;
		}
	}

	/**
	 * Switches between chart, Single score chart and table view
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class SwitchViewMode extends Action {

		/**
		 * Value for chart mode
		 */
		private static final int CHART = 1;

		/**
		 * Value for single score mode
		 */
		private static final int SINGLE_SCORE = 2;

		/**
		 * Value for table mode
		 */
		private static final int TABLE = 0;

		/**
		 * Indicates if the results should be displayed as a chart, a single
		 * score chart or in a table if running the action
		 */
		private final int type;

		/**
		 * Creates a new instance
		 * 
		 * @param type
		 *            Indicates if the results should be displayed as a chart, a
		 *            single score chart or in a table if running the action
		 */
		public SwitchViewMode(final int type) {
			this.type = type;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			switch (type) {
			case TABLE:
				return ImageType.TABLE_ICON.getDescriptor();
			case CHART:
				return ImageType.CHART_ICON.getDescriptor();
			case SINGLE_SCORE:
				return ImageType.SINGLE_SCORE_ICON.getDescriptor();
			default:
				return null;
			}
		}

		@Override
		public String getText() {
			String binding = null;
			switch (type) {
			case TABLE:
				binding = Messages.Results_Table;
				break;
			case CHART:
				binding = Messages.Results_Chart;
				break;
			case SINGLE_SCORE:
				binding = Messages.Results_SingleScoreChart;
				break;
			}
			final String text = NLS.bind(Messages.Results_ShowAs, binding);
			return text;
		}

		@Override
		public void run() {
			switch (type) {
			case TABLE:
				stackLayout.topControl = resultViewer.getTable();
				break;
			case CHART:
				stackLayout.topControl = chart;
				break;
			case SINGLE_SCORE:
				stackLayout.topControl = singleScoreChart;
				break;
			}
			stackLayout.topControl.getParent().layout();
		}
	}

}
