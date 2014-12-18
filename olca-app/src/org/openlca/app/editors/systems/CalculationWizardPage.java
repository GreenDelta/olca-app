package org.openlca.app.editors.systems;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.Messages;
import org.openlca.app.Preferences;
import org.openlca.app.db.Database;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationMethodViewer;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.combo.NwSetComboViewer;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.NwSetDao;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.NwSetDescriptor;
import org.slf4j.LoggerFactory;

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

	public CalculationSetup getSetup() {
		CalculationSetup setUp = new CalculationSetup(productSystem);
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
		loadDefaults();
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
			Controls.onSelect(radio, (e) -> {
				CalculationWizardPage.this.type = type;
				iterationText.setEnabled(type == CalculationType.MONTE_CARLO);
			});
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
			return Messages.Unknown;
		}
	}

	private void createAllocationViewer(Composite parent) {
		UI.formLabel(parent, Messages.AllocationMethod);
		allocationViewer = new AllocationMethodViewer(parent,
				AllocationMethod.values());
		allocationViewer.setNullable(false);
		allocationViewer.select(AllocationMethod.NONE);
	}

	private void createMethodComboViewer(Composite parent) {
		UI.formLabel(parent, Messages.ImpactAssessmentMethod);
		methodViewer = new ImpactMethodViewer(parent);
		methodViewer.setInput(Database.get());
		methodViewer.addSelectionChangedListener(
				(selection) -> nwViewer.setInput(methodViewer.getSelected()));
	}

	private void loadDefaults() {
		AllocationMethod allocationMethod = getDefaultAllocationMethod();
		allocationViewer.select(allocationMethod);
		ImpactMethodDescriptor method = getDefaultImpactMethod();
		if (method != null)
			methodViewer.select(method);
		NwSetDescriptor nwset = getDefaultNwSet();
		if (nwset != null)
			nwViewer.select(nwset);
	}

	private AllocationMethod getDefaultAllocationMethod() {
		String val = Preferences.get("calc.allocation.method");
		if (val == null)
			return AllocationMethod.NONE;
		for (AllocationMethod method : AllocationMethod.values()) {
			if (method.name().equals(val))
				return method;
		}
		return AllocationMethod.NONE;
	}

	private ImpactMethodDescriptor getDefaultImpactMethod() {
		String val = Preferences.get("calc.impact.method");
		if (val == null || val.isEmpty())
			return null;
		try {
			ImpactMethodDao dao = new ImpactMethodDao(Database.get());
			for (ImpactMethodDescriptor d : dao.getDescriptors()) {
				if (val.equals(d.getRefId()))
					return d;
			}
		} catch (Exception e) {
			LoggerFactory.getLogger(getClass()).error(
					"failed to load LCIA methods", e);
		}
		return null;
	}

	private NwSetDescriptor getDefaultNwSet() {
		String val = Preferences.get("calc.nwset");
		if (val == null || val.isEmpty())
			return null;
		ImpactMethodDescriptor method = methodViewer.getSelected();
		if (method == null)
			return null;
		try {
			NwSetDao dao = new NwSetDao(Database.get());
			for (NwSetDescriptor d : dao
					.getDescriptorsForMethod(method.getId())) {
				if (val.equals(d.getRefId()))
					return d;
			}
		} catch (Exception e) {
			LoggerFactory.getLogger(getClass()).error(
					"failed to load NW sets", e);
		}
		return null;
	}

}
