/*******************************************************************************
 * Copyright (c) 2007 - 2012 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.actions;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.ApplicationProperties;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.CalculationType;
import org.openlca.app.editors.DataBinding;
import org.openlca.app.editors.DataBinding.TextBindType;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.combo.AllocationMethodViewer;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.combo.NormalizationWeightingSetViewer;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.NormalizationWeightingSet;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;

/**
 * Page for setting the calculation properties of a product system. Class must
 * be public in order to allow data-binding.
 */
class CalculationWizardPage extends WizardPage {

	private AllocationMethodViewer allocationViewer;
	private ImpactMethodViewer methodViewer;
	private NormalizationWeightingSetViewer nwViewer;
	private Text iterationText;
	private int iterationCount = 100;
	private CalculationType type = CalculationType.QUICK;

	public CalculationWizardPage() {
		super(CalculationWizardPage.class.getCanonicalName());
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
		nwViewer.setDatabase(Database.get());

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
		new DataBinding().on(this, "iterationCount", TextBindType.INT,
				iterationText);
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
			return Messages.MonteCarloSimulation;
		case QUICK:
			return Messages.CalculationWizardPage_QuickResults;
		default:
			return "unknown";
		}
	}

	private void createAllocationViewer(Composite parent) {
		UI.formLabel(parent, Messages.AllocationMethod);
		allocationViewer = new AllocationMethodViewer(parent);
		allocationViewer.setNullable(true);
	}

	public CalculationSettings getSettings() {
		CalculationSettings settings = this.new CalculationSettings();
		settings.setAllocationMethod(allocationViewer.getSelected());
		settings.setMethod(methodViewer.getSelected());
		NormalizationWeightingSet set = nwViewer.getSelected();
		settings.setNwSet(set);
		settings.setType(type);
		settings.setIterationCount(iterationCount);
		return settings;
	}

	private void createMethodComboViewer(Composite parent) {
		UI.formLabel(parent, Messages.ImpactMethod);
		methodViewer = new ImpactMethodViewer(parent);
		methodViewer.setInput(Database.get());
		methodViewer
				.addSelectionChangedListener(new ISelectionChangedListener<ImpactMethodDescriptor>() {

					@Override
					public void selectionChanged(
							ImpactMethodDescriptor selection) {
						nwViewer.setInput(methodViewer.getSelected());
					}
				});
	}

	private void setDefaultData() {
		setDefaultAllocationMethod();
		// TODO set default method and nw set
	}

	private void setDefaultAllocationMethod() {
		String method = ApplicationProperties.PROP_DEFAULT_ALLOCATION_METHOD
				.getValue();
		if (method == null)
			return;
		allocationViewer.select(AllocationMethod.valueOf(method));
	}

	protected void reset() {
		if (allocationViewer != null)
			allocationViewer.select(null);
		if (methodViewer != null)
			methodViewer.select(null);
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

	class CalculationSettings {

		private NormalizationWeightingSet nwSet;
		private ImpactMethodDescriptor method;
		private AllocationMethod allocationMethod;
		private CalculationType type;
		private int iterationCount;

		public NormalizationWeightingSet getNwSet() {
			return nwSet;
		}

		public void setNwSet(NormalizationWeightingSet nwSet) {
			this.nwSet = nwSet;
		}

		public ImpactMethodDescriptor getMethod() {
			return method;
		}

		public void setMethod(ImpactMethodDescriptor method) {
			this.method = method;
		}

		public AllocationMethod getAllocationMethod() {
			return allocationMethod;
		}

		public void setAllocationMethod(AllocationMethod allocationMethod) {
			this.allocationMethod = allocationMethod;
		}

		public CalculationType getType() {
			return type;
		}

		public void setType(CalculationType type) {
			this.type = type;
		}

		public void setIterationCount(int iterationCount) {
			this.iterationCount = iterationCount;
		}

		public int getIterationCount() {
			return iterationCount;
		}

	}

}
