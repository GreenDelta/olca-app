package org.openlca.app.editors.systems;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.Preferences;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationMethodViewer;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.combo.NwSetComboViewer;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.NwSetDao;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.CalculationType;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.NwSetDescriptor;
import org.python.google.common.base.Strings;
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
	private Button costCheck;
	private Button dqAssessment;
	private Button storeInventoryResult;
	private Composite optionStack;
	private Composite monteCarloOptions;
	private Composite commonOptions;
	private Map<CalculationType, Button> calculationRadios = new HashMap<>();

	private CalculationType type;
	private int iterationCount;

	CalculationWizardPage() {
		super(CalculationWizardPage.class.getCanonicalName());
		setTitle(M.CalculationProperties);
		setDescription(M.CalculationWizardDescription);
		setImageDescriptor(Icon.CALCULATION_WIZARD.descriptor());
		setPageComplete(true);
	}

	@Override
	public void createControl(Composite parent) {
		Composite body = new Composite(parent, SWT.NULL);
		UI.gridLayout(body, 2, 10, 10);
		setControl(body);
		createSelections(body);
		new Label(body, SWT.NONE);
		UI.gridData(new Label(body, SWT.SEPARATOR | SWT.HORIZONTAL), true, false);
		new Label(body, SWT.NONE);
		optionStack = new Composite(body, SWT.NULL);
		StackLayout optionsLayout = new StackLayout();
		optionStack.setLayout(optionsLayout);
		createMonteCarloOptions(optionStack);
		createCommonOptions(optionStack);
		optionsLayout.topControl = commonOptions;
		new Label(body, SWT.NONE);
		loadDefaults();
	}

	private void createSelections(Composite parent) {
		createAllocationViewer(parent);
		createMethodComboViewer(parent);
		UI.formLabel(parent, M.NormalizationAndWeightingSet);
		nwViewer = new NwSetComboViewer(parent);
		nwViewer.setDatabase(Database.get());
		UI.formLabel(parent, M.CalculationType);
		createRadios(parent);
	}

	private void createRadios(Composite parent) {
		Composite composite = new Composite(parent, SWT.NO_RADIO_GROUP);
		CalculationType[] types = {
				CalculationType.CONTRIBUTION_ANALYSIS,
				CalculationType.UPSTREAM_ANALYSIS,
				CalculationType.REGIONALIZED_CALCULATION,
				CalculationType.MONTE_CARLO_SIMULATION,
		};
		UI.gridLayout(composite, types.length, 10, 0);
		for (CalculationType cType : types) {
			Button button = new Button(composite, SWT.RADIO);
			button.setText(getLabel(cType));
			calculationRadios.put(cType, button);
			Controls.onSelect(button, e -> {
				type = cType;
				updateOptions();
				for (Entry<CalculationType, Button> entry : calculationRadios.entrySet()) {
					Button b = entry.getValue();
					b.setSelection(cType == entry.getKey());
				}
			});
		}
	}

	private void updateOptions() {
		if (type == CalculationType.MONTE_CARLO_SIMULATION) {
			setTopControl(optionStack, monteCarloOptions);
		} else {
			setTopControl(optionStack, commonOptions);
		}
	}

	private void setTopControl(Composite stack, Composite control) {
		StackLayout layout = (StackLayout) stack.getLayout();
		layout.topControl = control;
		stack.layout();
	}

	private void createCommonOptions(Composite parent) {
		commonOptions = new Composite(parent, SWT.NULL);
		UI.gridLayout(commonOptions, 1, 10, 0);
		costCheck = new Button(commonOptions, SWT.CHECK);
		costCheck.setSelection(false);
		costCheck.setText(M.IncludeCostCalculation);
		dqAssessment = new Button(commonOptions, SWT.CHECK);
		dqAssessment.setSelection(false);
		dqAssessment.setText(M.AssessDataQuality);
		Controls.onSelect(dqAssessment, e -> dqAssessmentChanged());
		if (Database.isConnected()) {
			storeInventoryResult = new Button(commonOptions, SWT.CHECK);
			storeInventoryResult.setSelection(true);
			storeInventoryResult.setText(M.StoreInventoryResult);
		}
	}

	private void createMonteCarloOptions(Composite parent) {
		monteCarloOptions = new Composite(parent, SWT.NULL);
		UI.gridLayout(monteCarloOptions, 2, 10, 0);
		Label label = UI.formLabel(monteCarloOptions, M.NumberOfIterations);
		UI.gridData(label, false, false);
		iterationText = new Text(monteCarloOptions, SWT.BORDER);
		UI.gridData(iterationText, false, false).widthHint = 80;
		iterationText.addModifyListener(e -> numberOfRunsChanged());
	}

	private void numberOfRunsChanged() {
		String text = iterationText.getText();
		try {
			iterationCount = Integer.parseInt(text);
		} catch (Exception e2) {
			Error.showBox(M.InvalidNumber, text + " " + M.IsNotValidNumber);
		}
	}

	private String getLabel(CalculationType type) {
		switch (type) {
		case UPSTREAM_ANALYSIS:
			return M.Analysis;
		case MONTE_CARLO_SIMULATION:
			return M.MonteCarloSimulation;
		case CONTRIBUTION_ANALYSIS:
			return M.QuickResults;
		case REGIONALIZED_CALCULATION:
			return M.RegionalizedLCIA;
		default:
			return M.Unknown;
		}
	}

	private void createAllocationViewer(Composite parent) {
		UI.formLabel(parent, M.AllocationMethod);
		allocationViewer = new AllocationMethodViewer(parent, AllocationMethod.values());
		allocationViewer.setNullable(false);
		allocationViewer.select(AllocationMethod.NONE);
	}

	private void createMethodComboViewer(Composite parent) {
		UI.formLabel(parent, M.ImpactAssessmentMethod);
		methodViewer = new ImpactMethodViewer(parent);
		methodViewer.setNullable(true);
		methodViewer.setInput(Database.get());
		methodViewer.addSelectionChangedListener((selection) -> nwViewer.setInput(methodViewer.getSelected()));
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
		type = getDefaultValue(CalculationType.class,
				CalculationType.CONTRIBUTION_ANALYSIS);
		calculationRadios.get(type).setSelection(true);
		String itCount = Preferences.get("calc.numberOfRuns");
		if (Strings.isNullOrEmpty(itCount))
			itCount = "100";
		iterationText.setText(itCount);
		boolean withCosts = getDefaultBoolean("calc.costCalculation");
		costCheck.setSelection(withCosts);
		boolean doDqAssessment = getDefaultBoolean("calc.dqAssessment");
		dqAssessment.setSelection(doDqAssessment);
		updateOptions();
		if (type == CalculationType.MONTE_CARLO_SIMULATION)
			return;
		dqAssessmentChanged();
	}

	private void dqAssessmentChanged() {
		if (getControl().isVisible())
			getContainer().updateButtons();
	}

	@Override
	public boolean canFlipToNextPage() {
		if (!isPageComplete())
			return false;
		return dqAssessment.getSelection();
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
			LoggerFactory.getLogger(getClass()).error("failed to load LCIA methods", e);
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
			for (NwSetDescriptor d : dao.getDescriptorsForMethod(method.getId())) {
				if (val.equals(d.getRefId()))
					return d;
			}
		} catch (Exception e) {
			LoggerFactory.getLogger(getClass()).error("failed to load NW sets", e);
		}
		return null;
	}

	private boolean getDefaultBoolean(String option) {
		String value = Preferences.get(option);
		if (value == null)
			return false;
		return "true".equals(value.toLowerCase());
	}

	private <T extends Enum<T>> T getDefaultValue(Class<T> type, T defaultValue) {
		String name = Preferences.get("calc." + type.getSimpleName());
		if (Strings.isNullOrEmpty(name))
			return defaultValue;
		try {
			T value = Enum.valueOf(type, name);
			if (value != null)
				return value;
		} catch (Exception e) {
		}
		return defaultValue;
	}

	CalculationSetup getSetup(ProductSystem system) {
		CalculationSetup setUp = new CalculationSetup(type, system);
		setUp.withCosts = costCheck.getSelection();
		setUp.allocationMethod = allocationViewer.getSelected();
		setUp.impactMethod = methodViewer.getSelected();
		NwSetDescriptor set = nwViewer.getSelected();
		setUp.nwSet = set;
		setUp.numberOfRuns = iterationCount;
		setUp.parameterRedefs.addAll(system.parameterRedefs);
		return setUp;
	}

	boolean doDqAssessment() {
		return dqAssessment.getSelection();
	}

	boolean doStoreInventoryResult() {
		if (!Database.isConnected() || storeInventoryResult == null)
			return false;
		return storeInventoryResult.getSelection();
	}

}
