/*******************************************************************************
 * Copyright (c) 2007 - 2012 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.core.application.ApplicationProperties;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.MethodDao;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.NormalizationWeightingSet;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.DataBinding;
import org.openlca.ui.UI;
import org.openlca.ui.viewer.AllocationMethodViewer;
import org.openlca.ui.viewer.ImpactMethodViewer;
import org.openlca.ui.viewer.NormalizationWeightingSetViewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Page for setting the calculation properties of a product system. Class must
 * be public in order to allow data-binding.
 */
public class CalculationWizardPage extends WizardPage {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public static String ID = "org.openlca.core.application.wizards.CalculationWizardPage";
	private IDatabase database;
	private AllocationMethodViewer allocationViewer;
	private ImpactMethodViewer methodViewer;
	private NormalizationWeightingSetViewer nwViewer;
	private Text iterationText;
	private int iterationCount = 100;
	private CalculationType type = CalculationType.QUICK;

	public CalculationWizardPage(IDatabase database) {
		super(ID);
		this.database = database;
		setTitle(Messages.CalculationWizardTitle);
		setDescription(Messages.CalculationWizardDescription);
		setImageDescriptor(ImageType.WIZ_CALCULATION.getDescriptor());
		setPageComplete(true);
	}

	public int getIterationCount() {
		return iterationCount;
	}

	public void setIterationCount(int iterationCount) {
		this.iterationCount = iterationCount;
	}

	@Override
	public void createControl(Composite parent) {
		Composite body = new Composite(parent, SWT.NULL);
		setControl(body);
		UI.gridLayout(body, 2).verticalSpacing = 12;
		createAllocationViewer(body);
		createMethodComboViewer(body);
		UI.formLabel(body, Messages.NWSet);
		nwViewer = new NormalizationWeightingSetViewer(body);
		nwViewer.setDatabase(database);

		UI.formLabel(body, Messages.CalculationWizardPage_CalculationType);
		Composite typePanel = new Composite(body, SWT.NONE);
		UI.gridLayout(typePanel, 2).horizontalSpacing = 15;
		createRadios(typePanel);
		createIterationText(typePanel);
		setDefaultData();
	}

	private void createIterationText(Composite typePanel) {
		Label label = UI.formLabel(typePanel,
				Messages.CalculationWizardPage_NumberOfIterations);
		UI.gridData(label, false, false).horizontalIndent = 16;
		iterationText = new Text(typePanel, SWT.BORDER);
		UI.gridData(iterationText, false, false).widthHint = 80;
		iterationText.setEnabled(false);
		new DataBinding().onInt(this, "iterationCount", iterationText);
	}

	private void createRadios(Composite parent) {
		CalculationType[] types = { CalculationType.QUICK,
				CalculationType.ANALYSIS, CalculationType.MONTE_CARLO };
		for (CalculationType type : types) {
			Button radio = new Button(parent, SWT.RADIO);
			radio.setSelection(type == this.type);
			radio.setText(getLabel(type));
			UI.gridData(radio, false, false).horizontalSpan = 2;
			radio.addSelectionListener(new CalculationTypeChange(type));
		}
	}

	private String getLabel(CalculationType type) {
		switch (type) {
		case ANALYSIS:
			return Messages.CalculationWizardPage_Analysis;
		case MONTE_CARLO:
			return Messages.Common_MonteCarloSimulation;
		case QUICK:
			return Messages.CalculationWizardPage_QuickResults;
		default:
			return "unknown";
		}
	}

	private void createAllocationViewer(Composite parent) {
		UI.formLabel(parent, Messages.AllocationMethod);
		allocationViewer = new AllocationMethodViewer(parent,
				AllocationMethodViewer.APPEND_INHERIT_OPTION);
	}

	public CalculationSettings getSettings() {
		CalculationSettings settings = new CalculationSettings();
		settings.setAllocationMethod(allocationViewer.getSelected());
		settings.setMethod(methodViewer.getSelected());
		NormalizationWeightingSet set = nwViewer.getSelected();
		settings.setNwSet(set);
		settings.setType(type);
		settings.setIterationCount(iterationCount);
		return settings;
	}

	private void createMethodComboViewer(Composite parent) {
		UI.formLabel(parent, Messages.LCIAMethod);
		methodViewer = new ImpactMethodViewer(parent);
		methodViewer.setInput(database);
		methodViewer
				.addSelectionChangedListener(new org.openlca.ui.viewer.ISelectionChangedListener<ImpactMethodDescriptor>() {

					@Override
					public void selectionChanged(
							ImpactMethodDescriptor selection) {
						initNwSets();
					}
				});
	}

	private void initNwSets() {
		nwViewer.setInput(methodViewer.getSelected());
		String defaultNwId = getDefaultId(ApplicationProperties.PROP_DEFAULT_NORMALIZATION_WEIGHTING_SET);
		if (defaultNwId == null)
			return;
		nwViewer.select(nwViewer.find(defaultNwId));
	}

	private void setDefaultData() {
		setDefaultAllocationMethod();
		setDefaultMethod();
	}

	private void setDefaultMethod() {
		String id = getDefaultId(ApplicationProperties.PROP_DEFAULT_LCIA_METHOD);
		if (id == null)
			return;
		try {
			ImpactMethodDescriptor method = new MethodDao(
					database.getEntityFactory()).getDescriptor(id);
			if (method != null) {
				methodViewer.select(method);
				initNwSets();
			}
		} catch (Exception e) {
			log.error("Loading method descriptor failed", e);
		}
	}

	private String getDefaultId(ApplicationProperties prop) {
		return prop.getValue(database.getUrl());
	}

	private void setDefaultAllocationMethod() {
		String method = ApplicationProperties.PROP_DEFAULT_ALLOCATION_METHOD
				.getValue();
		if (method == null)
			return;
		allocationViewer.select(AllocationMethod.valueOf(method));
	}

	protected void reset() {
		if (allocationViewer != null) {
			allocationViewer.select(null);
		}
		if (methodViewer != null) {
			methodViewer.select(null);
		}
		if (nwViewer != null) {
			nwViewer.select(null);
			nwViewer.setInput(null);
		}
		setDefaultData();
	}

	private class CalculationTypeChange implements SelectionListener {

		private CalculationType type;

		public CalculationTypeChange(CalculationType type) {
			this.type = type;
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			CalculationWizardPage.this.type = type;
			iterationText.setEnabled(type == CalculationType.MONTE_CARLO);
		}
	}

}
