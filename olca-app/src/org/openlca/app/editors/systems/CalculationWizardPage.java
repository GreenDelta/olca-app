package org.openlca.app.editors.systems;

import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.Preferences;
import org.openlca.app.db.Database;
import org.openlca.app.editors.processes.DQSystemViewer;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Error;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationMethodViewer;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.combo.NwSetComboViewer;
import org.openlca.core.database.DQSystemDao;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.NwSetDao;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.AggregationType;
import org.openlca.core.math.data_quality.DQCalculationSetup;
import org.openlca.core.math.data_quality.ProcessingType;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.DQSystemDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
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
	private DQSystemViewer processSystemViewer;
	private DQSystemViewer exchangeSystemViewer;
	private Text iterationText;
	private Button costCheck;
	private Button dqAssessment;
	private Composite optionStack;
	private Composite monteCarloOptions;
	private Composite commonOptions;
	private Composite dqOptionStack;
	private Composite dqOptions;
	private Map<CalculationType, Button> calculationRadios = new HashMap<>();
	private Map<AggregationType, Button> aggregationRadios = new HashMap<>();
	private Map<ProcessingType, Button> processingRadios = new HashMap<>();
	private Map<RoundingMode, Button> roundingRadios = new HashMap<>();

	private CalculationType type;
	private AggregationType aggregationType;
	private ProcessingType processingType;
	private RoundingMode roundingMode;
	private int iterationCount;
	private boolean dqSystemsLoaded;
	private ProductSystem productSystem;

	public CalculationWizardPage(ProductSystem system) {
		super(CalculationWizardPage.class.getCanonicalName());
		this.productSystem = system;
		setTitle(M.CalculationProperties);
		setDescription(M.CalculationWizardDescription);
		setImageDescriptor(Icon.CALCULATION_WIZARD.descriptor());
		setPageComplete(true);
	}

	public CalculationSetup getSetup() {
		CalculationSetup setUp = new CalculationSetup(productSystem);
		setUp.withCosts = costCheck.getSelection();
		setUp.allocationMethod = allocationViewer.getSelected();
		setUp.impactMethod = methodViewer.getSelected();
		NwSetDescriptor set = nwViewer.getSelected();
		setUp.nwSet = set;
		setUp.numberOfRuns = iterationCount;
		setUp.parameterRedefs.addAll(productSystem.getParameterRedefs());
		return setUp;
	}

	public DQCalculationSetup getDqSetup() {
		if (!dqAssessment.getSelection())
			return null;
		long psId = productSystem.getId();
		DQSystemDao dqDao = new DQSystemDao(Database.get());
		DQSystem pSystem = null;
		DQSystemDescriptor pSystemDesc = processSystemViewer.getSelected();
		if (pSystemDesc != null)
			pSystem = dqDao.getForId(pSystemDesc.getId());
		DQSystem eSystem = null;
		DQSystemDescriptor eSystemDesc = exchangeSystemViewer.getSelected();
		if (eSystemDesc != null)
			eSystem = dqDao.getForId(eSystemDesc.getId());
		return new DQCalculationSetup(psId, aggregationType, roundingMode, processingType, pSystem, eSystem);
	}

	public CalculationType getCalculationType() {
		return type;
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
		createDqOptions(body);
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
		Composite composite = new Composite(parent, SWT.NULL);
		CalculationType[] types = getCalculationTypes();
		UI.gridLayout(composite, types.length, 10, 0);
		for (CalculationType type : types) {
			createTypeRadio(composite, getLabel(type), calculationRadios, type, 1);
		}
	}

	private <T extends Enum<T>> void createTypeRadio(Composite c, String label, Map<T, Button> radios, T value, int cols) {
		Button button = new Button(c, SWT.RADIO);
		button.setText(label);
		Controls.onSelect(button, e -> typeChanged(radios, value));
		UI.gridData(button, false, false).horizontalSpan = cols;
		radios.put(value, button);
	}

	private <T extends Enum<T>> void typeChanged(Map<T, Button> radios, T value) {
		if (value instanceof AggregationType) {
			aggregationType = (AggregationType) value;
		} else if (value instanceof RoundingMode) {
			roundingMode = (RoundingMode) value;
		} else if (value instanceof ProcessingType) {
			processingType = (ProcessingType) value;
		} else if (value instanceof CalculationType) {
			type = (CalculationType) value;
			updateOptions();
		}
		for (Entry<T, Button> entry : radios.entrySet()) {
			Button button = entry.getValue();
			button.setSelection(value == entry.getKey());
		}
	}

	private void updateOptions() {
		if (type == CalculationType.MONTE_CARLO) {
			setTopControl(optionStack, monteCarloOptions);
			setTopControl(dqOptionStack, null);
		} else {
			setTopControl(optionStack, commonOptions);
			if (dqAssessment.getSelection()) {
				setTopControl(dqOptionStack, dqOptions);
			} else {
				setTopControl(dqOptionStack, null);
			}
		}
	}

	private void createCommonOptions(Composite parent) {
		commonOptions = new Composite(parent, SWT.NULL);
		UI.gridLayout(commonOptions, 1, 10, 0);
		costCheck = new Button(commonOptions, SWT.CHECK);
		costCheck.setSelection(false);
		costCheck.setText(M.IncludeCostCalculation);
		dqAssessment = new Button(commonOptions, SWT.CHECK);
		dqAssessment.setSelection(false);
		dqAssessment.setText("#Assess data quality");
		Controls.onSelect(dqAssessment, e -> dqAssessmentChanged());
	}

	private void dqAssessmentChanged() {
		if (dqAssessment.getSelection()) {
			loadDqSystems();
			setTopControl(dqOptionStack, dqOptions);
		} else {
			setTopControl(dqOptionStack, null);
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

	private void createDqOptions(Composite parent) {
		dqOptionStack = new Composite(parent, SWT.NULL);
		UI.gridData(dqOptionStack, true, false);
		StackLayout layout = new StackLayout();
		dqOptionStack.setLayout(layout);
		dqOptions = new Composite(dqOptionStack, SWT.NULL);
		UI.gridLayout(dqOptions, 1, 10, 0);
		UI.gridData(new Label(dqOptions, SWT.SEPARATOR | SWT.HORIZONTAL), true, false);
		// otherwise all radios will be handled as one group
		Composite container = new Composite(dqOptions, SWT.NO_RADIO_GROUP);
		UI.gridLayout(container, 5, 10, 0);
		UI.gridData(container, true, false);
		processSystemViewer = createDQSystemViewer(container, "#Process schema:", 4);
		exchangeSystemViewer = createDQSystemViewer(container, "#I/O schema:", 4);
		new Label(container, SWT.NULL).setText("#Aggregation type:");
		for (AggregationType type : AggregationType.values()) {
			createTypeRadio(container, Labels.aggregationType(type), aggregationRadios, type, 1);
		}
		new Label(container, SWT.NULL).setText("#Rounding mode:");
		createTypeRadio(container, "#Half up", roundingRadios, RoundingMode.HALF_UP, 1);
		createTypeRadio(container, "#Up", roundingRadios, RoundingMode.CEILING, 3);
		new Label(container, SWT.NULL).setText("#n.a. value handling:");
		createTypeRadio(container, Labels.processingType(ProcessingType.EXCLUDE), processingRadios,
				ProcessingType.EXCLUDE, 1);
		createTypeRadio(container, Labels.processingType(ProcessingType.USE_MAX), processingRadios,
				ProcessingType.USE_MAX, 3);
	}

	private CalculationType[] getCalculationTypes() {
		return new CalculationType[] { CalculationType.QUICK,
				CalculationType.ANALYSIS, CalculationType.REGIONALIZED,
				CalculationType.MONTE_CARLO };
	}

	private String getLabel(CalculationType type) {
		switch (type) {
		case ANALYSIS:
			return M.Analysis;
		case MONTE_CARLO:
			return M.MonteCarloSimulation;
		case QUICK:
			return M.QuickResults;
		case REGIONALIZED:
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

	private DQSystemViewer createDQSystemViewer(Composite parent, String label, int cols) {
		UI.formLabel(parent, label);
		DQSystemViewer viewer = new DQSystemViewer(parent);
		GridData gd = (GridData) viewer.getViewer().getTableCombo().getLayoutData();
		gd.horizontalSpan = cols;
		return viewer;
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
		type = getDefaultValue(CalculationType.class, CalculationType.QUICK);
		calculationRadios.get(type).setSelection(true);
		updateOptions();
		boolean withCosts = getDefaultBoolean("calc.costCalculation");
		costCheck.setSelection(withCosts);
		boolean doDqAssessment = getDefaultBoolean("calc.dqAssessment");
		dqAssessment.setSelection(doDqAssessment);
		dqAssessmentChanged();
		AggregationType aType = getDefaultValue(AggregationType.class, AggregationType.WEIGHTED_AVERAGE);
		aggregationRadios.get(aType).setSelection(true);
		typeChanged(aggregationRadios, aType);
		ProcessingType pType = getDefaultValue(ProcessingType.class, ProcessingType.EXCLUDE);
		processingRadios.get(pType).setSelection(true);
		typeChanged(processingRadios, pType);
		RoundingMode rounding = getDefaultValue(RoundingMode.class, RoundingMode.HALF_UP);
		roundingRadios.get(rounding).setSelection(true);
		typeChanged(roundingRadios, rounding);
		String itCount = Preferences.get("calc.numberOfRuns");
		if (Strings.isNullOrEmpty(itCount))
			itCount = "100";
		iterationText.setText(itCount);
	}

	private void loadDqSystems() {
		if (dqSystemsLoaded)
			return;
		DQSystemDao dao = new DQSystemDao(Database.get());
		processSystemViewer.setInput(dao.getProcessDqSystems(productSystem.getId()));
		exchangeSystemViewer.setInput(dao.getExchangeDqSystems(productSystem.getId()));
		DQSystem processSystem = productSystem.getReferenceProcess().dqSystem;
		DQSystem exchangeSystem = productSystem.getReferenceProcess().exchangeDqSystem;
		if (processSystem != null) {
			processSystemViewer.select(Descriptors.toDescriptor(processSystem));
		} else {
			processSystemViewer.selectFirst();
		}
		if (exchangeSystem != null) {
			exchangeSystemViewer.select(Descriptors.toDescriptor(exchangeSystem));
		} else {
			exchangeSystemViewer.selectFirst();
		}
		dqSystemsLoaded = true;
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

	private <T extends Enum<T>> T getDefaultValue(Class<T> type, T defaultValue) {
		String name = Preferences.get("calc." + type.getSimpleName());
		if (Strings.isNullOrEmpty(name))
			return defaultValue;
		T value = Enum.valueOf(type, name);
		if (value == null)
			return defaultValue;
		return value;
	}

	private boolean getDefaultBoolean(String option) {
		String value = Preferences.get(option);
		if (value == null)
			return false;
		return "true".equals(value.toLowerCase());
	}

	private void setTopControl(Composite stack, Composite control) {
		StackLayout layout = (StackLayout) stack.getLayout();
		layout.topControl = control;
		stack.layout();
	}

}
