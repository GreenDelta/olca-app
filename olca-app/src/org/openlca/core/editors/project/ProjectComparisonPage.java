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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.MethodDao;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.model.LCIACategory;
import org.openlca.core.model.LCIAMethod;
import org.openlca.core.model.NormalizationWeightingSet;
import org.openlca.core.model.Project;
import org.openlca.ui.BaseLabelProvider;
import org.openlca.ui.BaseNameSorter;
import org.openlca.ui.UIFactory;
import org.openlca.ui.Viewers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FormPage to compare product systems of a project
 * 
 * @author Sebastian Greve
 * 
 */
public class ProjectComparisonPage extends ModelEditorPage {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public static final int CHARACTERIZATION = 0;
	public static final int NORMALIZATION = 1;
	public static final int SINGLE_SCORE = 3;
	public static final int WEIGHTING = 2;
	private int type = CHARACTERIZATION;

	/**
	 * Button for drawing the chart
	 */
	private Button button;

	/**
	 * Radio button to select charactization for chart values
	 */
	private Button characterizationRadio;
	/**
	 * The comparison chart
	 */
	private ProductSystemComparisonChart chart;
	/**
	 * Combo viewer for selecting an LCIA method
	 */
	private ComboViewer methodViewer;

	/**
	 * Radio button to select normalization for chart values
	 */
	private Button normalizationRadio;

	/**
	 * the project object edited by this editor
	 */
	private Project project = null;

	/**
	 * The composite containing the radio buttons
	 */
	private Composite radioComposite;

	/**
	 * The selected LCIA categories
	 */
	private LCIACategory[] selectedCategories;

	/**
	 * The selected LCIA method
	 */
	private LCIAMethod selectedMethod;

	/**
	 * The selected normalization and weighting set
	 */
	private NormalizationWeightingSet selectedNwSet;

	/**
	 * Indicates if the double values should be displayed in the chart
	 */
	private boolean showValues;

	/**
	 * Radio button to select single score for chart values
	 */
	private Button singleScoreRadio;

	/**
	 * Radio button to select weighting for chart values
	 */
	private Button weightingRadio;

	/**
	 * Creates a new instance.
	 * 
	 * @param editor
	 *            the editor of this page
	 */
	public ProjectComparisonPage(final ModelEditor editor) {
		super(editor, "ProjectComparisonPage",
				Messages.Projects_ProjectComparisonPageLabel);
		this.project = (Project) editor.getModelComponent();
	}

	@Override
	protected void createContents(final Composite body,
			final FormToolkit toolkit) {
		final Section section = UIFactory.createSection(body, toolkit,
				Messages.Projects_ProjectComparisonPageLabel, true, false);

		final Composite composite = UIFactory.createSectionComposite(section,
				toolkit, UIFactory.createGridLayout(3, false, 5));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		new Label(composite, SWT.NONE).setText(Messages.Projects_LCIAMethod);
		methodViewer = new ComboViewer(composite);
		methodViewer.setContentProvider(ArrayContentProvider.getInstance());
		methodViewer.setLabelProvider(new BaseLabelProvider());
		methodViewer.setSorter(new BaseNameSorter());
		methodViewer.getCombo().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, false));

		button = toolkit.createButton(composite, Messages.Projects_DrawChart,
				SWT.NONE);
		button.setEnabled(false);

		final Section chartSection = UIFactory.createSection(body, toolkit,
				Messages.Projects_ChartSectionLabel, true, true);
		final Composite chartComposite = UIFactory.createSectionComposite(
				chartSection, toolkit, UIFactory.createGridLayout(1, true, 0));

		radioComposite = toolkit.createComposite(chartComposite);
		radioComposite.setLayout(UIFactory.createGridLayout(4, false, 5));
		radioComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		characterizationRadio = toolkit.createButton(radioComposite,
				Messages.Projects_Characterization, SWT.RADIO);
		characterizationRadio.setSelection(true);

		normalizationRadio = toolkit.createButton(radioComposite,
				Messages.Projects_Normalization, SWT.RADIO);

		weightingRadio = toolkit.createButton(radioComposite,
				Messages.Projects_Weighting, SWT.RADIO);

		singleScoreRadio = toolkit.createButton(radioComposite,
				Messages.Projects_SingleScore, SWT.RADIO);
		radioComposite.setVisible(false);

		chart = new ProductSystemComparisonChart(chartComposite, project,
				getDatabase());
		chart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	@Override
	protected String getFormTitle() {
		final String title = Messages.Projects_FormText
				+ ": "
				+ (project != null ? project.getName() != null ? project
						.getName() : "" : "");
		return title;
	}

	@Override
	protected void initListeners() {

		button.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				// no action on default selection
			}

			@Override
			public void widgetSelected(final SelectionEvent e) {
				try {
					selectedMethod = Viewers.getFirstSelected(methodViewer);
					DrawOptionDialog dialog = new DrawOptionDialog(
							selectedMethod);
					if (dialog.open() == Window.OK) {
						selectedCategories = dialog.getSelectedCategories();
						selectedNwSet = dialog.getNormalizationWeightingSet();
						showValues = dialog.showValuesOnBarSeries();
						chart.setData(selectedMethod, selectedNwSet,
								selectedCategories, showValues, type);
						radioComposite.setVisible(true);
						normalizationRadio.setVisible(selectedNwSet != null);
						weightingRadio.setVisible(selectedNwSet != null);
						singleScoreRadio.setVisible(selectedNwSet != null);
					}
				} catch (Exception ex) {
					log.error("Failed to set data", ex);
				}
			}
		});

		methodViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						button.setEnabled(true);
					}
				});

		characterizationRadio.addSelectionListener(new DataSelectionDelegate(
				CHARACTERIZATION));
		normalizationRadio.addSelectionListener(new DataSelectionDelegate(
				NORMALIZATION));
		weightingRadio
				.addSelectionListener(new DataSelectionDelegate(WEIGHTING));
		singleScoreRadio.addSelectionListener(new DataSelectionDelegate(
				SINGLE_SCORE));
	}

	@Override
	protected void setData() {
		try {
			IDatabase database = getDatabase();
			MethodDao dao = new MethodDao(database.getEntityFactory());
			methodViewer.setInput(dao.getAll());
		} catch (Exception e) {
			log.error("Reading LCIA method from db failed", e);
		}
	}

	private class DataSelectionDelegate implements SelectionListener {

		private int type;

		public DataSelectionDelegate(int type) {
			this.type = type;
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			chart.setData(selectedMethod, selectedNwSet, selectedCategories,
					showValues, this.type);
		}
	}
}
