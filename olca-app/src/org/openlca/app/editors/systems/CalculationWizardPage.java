package org.openlca.app.editors.systems;

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
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationMethodViewer;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.combo.NwSetComboViewer;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.NwSetDescriptor;

/**
 * Page for setting the calculation properties of a product system. Class must
 * be public in order to allow data-binding.
 */
class CalculationWizardPage extends WizardPage {

	private AllocationMethodViewer allocationViewer;
	private ImpactMethodViewer methodViewer;
	private NwSetComboViewer nwViewer;
	private Text iterationText;
	private int iterationCount = 100;
	private CalculationType type = CalculationType.QUICK;
	private ProductSystem productSystem;

	public CalculationWizardPage(ProductSystem system) {
		super(CalculationWizardPage.class.getCanonicalName());
		this.productSystem = system;
		setTitle(Messages.CalculationProperties);
		setDescription(Messages.CalculationWizardDescription);
		setImageDescriptor(ImageType.WIZ_CALCULATION.getDescriptor());
		setPageComplete(true);
	}

	@Override
	public void createControl(Composite parent) {
		Composite body = new Composite(parent, SWT.NULL);
		setControl(body);
		UI.gridLayout(body, 2).verticalSpacing = 12;
		createAllocationViewer(body);
		createMethodComboViewer(body);
		UI.formLabel(body, Messages.NormalizationAndWeightingSet);
		nwViewer = new NwSetComboViewer(body);
		nwViewer.setDatabase(Database.get());

		UI.formLabel(body, Messages.CalculationType);
		Composite typePanel = new Composite(body, SWT.NONE);
		UI.gridLayout(typePanel, 2).horizontalSpacing = 15;
		createRadios(typePanel);
		createIterationText(typePanel);
		setDefaultData();
	}

	private void createIterationText(Composite typePanel) {
		Label label = UI.formLabel(typePanel,
				Messages.NumberOfIterations);
		UI.gridData(label, false, false).horizontalIndent = 16;
		iterationText = new Text(typePanel, SWT.BORDER);
		UI.gridData(iterationText, false, false).widthHint = 80;
		iterationText.setEnabled(false);
		iterationText.setText(Integer.toString(iterationCount));
		iterationText.addModifyListener((e) -> {
			String text = iterationText.getText();
			try {
				iterationCount = Integer.parseInt(text);
			} catch (Exception e2) {
				Error.showBox(Messages.InvalidNumber, text + " "
						+ Messages.IsNotValidNumber);
			}
		});
	}

	private void createRadios(Composite parent) {
		CalculationType[] types = getCalculationTypes();
		for (CalculationType type : types) {
			Button radio = new Button(parent, SWT.RADIO);
			radio.setSelection(type == this.type);
			radio.setText(getLabel(type));
			UI.gridData(radio, false, false).horizontalSpan = 2;
			radio.addSelectionListener(new CalculationTypeChange(type));
		}
	}

	private CalculationType[] getCalculationTypes() {
		if (FeatureFlag.LOCALISED_LCIA.isEnabled())
			return new CalculationType[] { CalculationType.QUICK,
					CalculationType.ANALYSIS, CalculationType.REGIONALIZED,
					CalculationType.MONTE_CARLO };
		else
			return new CalculationType[] { CalculationType.QUICK,
					CalculationType.ANALYSIS, CalculationType.MONTE_CARLO };
	}

	private String getLabel(CalculationType type) {
		switch (type) {
		case ANALYSIS:
			return Messages.Analysis;
		case MONTE_CARLO:
			return Messages.MonteCarloSimulation;
		case QUICK:
			return Messages.QuickResults;
		case REGIONALIZED:
			return Messages.RegionalizedLCIA;
		default:
			return "unknown";
		}
	}

	private void createAllocationViewer(Composite parent) {
		UI.formLabel(parent, Messages.AllocationMethod);
		allocationViewer = new AllocationMethodViewer(parent,
				AllocationMethod.values());
		allocationViewer.setNullable(false);
		allocationViewer.select(AllocationMethod.NONE);
	}

	public CalculationSetup getSetup() {
		CalculationSetup setUp = new CalculationSetup(productSystem,
				getSetupType());
		setUp.setAllocationMethod(allocationViewer.getSelected());
		setUp.setImpactMethod(methodViewer.getSelected());
		NwSetDescriptor set = nwViewer.getSelected();
		setUp.setNwSet(set);
		setUp.setNumberOfRuns(iterationCount);
		setUp.getParameterRedefs().addAll(productSystem.getParameterRedefs());
		return setUp;
	}

	public CalculationType getCalculationType() {
		return type;
	}

	private int getSetupType() {
		if (type == null)
			return CalculationSetup.QUICK_RESULT;
		switch (type) {
		case ANALYSIS:
			return CalculationSetup.ANALYSIS;
		case MONTE_CARLO:
			return CalculationSetup.MONTE_CARLO_SIMULATION;
		case QUICK:
			return CalculationSetup.QUICK_RESULT;
		default:
			return CalculationSetup.QUICK_RESULT;
		}
	}

	private void createMethodComboViewer(Composite parent) {
		UI.formLabel(parent, Messages.ImpactAssessmentMethod);
		methodViewer = new ImpactMethodViewer(parent);
		methodViewer.setInput(Database.get());
		methodViewer.addSelectionChangedListener(
				(selection) -> nwViewer.setInput(methodViewer.getSelected()));
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
			nwViewer.setInput((ImpactMethodDescriptor) null);
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
