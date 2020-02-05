package org.openlca.app.wizards.calculation;

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
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationMethodViewer;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.combo.NwSetComboViewer;
import org.openlca.core.math.CalculationType;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.python.google.common.base.Strings;

/**
 * Page for setting the calculation properties of a product system. Class must
 * be public in order to allow data-binding.
 */
class CalculationWizardPage extends WizardPage {

	private final Setup setup;

	private NwSetComboViewer nwViewer;
	private Composite optionStack;
	private Composite monteCarloOptions;
	private Composite commonOptions;

	CalculationWizardPage(Setup setup) {
		super("CalculationWizardPage");
		this.setup = setup;
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

		// main selectors
		createAllocationCombo(body);
		createMethodCombo(body);
		createNWSetCombo(body);
		createTypeRadios(body);

		// separator
		new Label(body, SWT.NONE);
		UI.gridData(new Label(
				body, SWT.SEPARATOR | SWT.HORIZONTAL),
				true, false);
		new Label(body, SWT.NONE);

		// options
		optionStack = new Composite(body, SWT.NULL);
		StackLayout optionsLayout = new StackLayout();
		optionStack.setLayout(optionsLayout);
		createMonteCarloOptions(optionStack);
		createCommonOptions(optionStack);
		optionsLayout.topControl = commonOptions;
		new Label(body, SWT.NONE);

		updateOptions();
		dqAssessmentChanged();
	}

	private void createNWSetCombo(Composite parent) {
		UI.formLabel(parent, M.NormalizationAndWeightingSet);
		nwViewer = new NwSetComboViewer(parent);
		nwViewer.setDatabase(Database.get());
		if (setup.calcSetup.nwSet != null) {
			nwViewer.select(setup.calcSetup.nwSet);
		}
		nwViewer.addSelectionChangedListener(nwSet -> {
			setup.calcSetup.nwSet = nwSet;
		});
	}

	private void createTypeRadios(Composite parent) {
		CalculationType[] types = {
				CalculationType.CONTRIBUTION_ANALYSIS,
				CalculationType.UPSTREAM_ANALYSIS,
				CalculationType.MONTE_CARLO_SIMULATION,
		};

		UI.formLabel(parent, M.CalculationType);
		Composite comp = new Composite(parent, SWT.NO_RADIO_GROUP);
		UI.gridLayout(comp, types.length, 10, 0);

		Button[] radios = new Button[types.length];
		for (int i = 0; i < types.length; i++) {
			Button radio = new Button(comp, SWT.RADIO);
			radio.setText(getLabel(types[i]));
			radio.setSelection(setup.calcType == types[i]);
			radios[i] = radio;
			Controls.onSelect(radio, e -> {
				for (int j = 0; j < types.length; j++) {
					if (radios[j] == radio) {
						radio.setSelection(true);
						setup.calcType = types[j];
					} else {
						radios[j].setSelection(false);
					}
				}
				updateOptions();
			});
		}
	}

	private void updateOptions() {
		StackLayout layout = (StackLayout) optionStack.getLayout();
		if (setup.calcType == CalculationType.MONTE_CARLO_SIMULATION) {
			layout.topControl = monteCarloOptions;
		} else {
			layout.topControl = commonOptions;
		}
		optionStack.layout();
	}

	private void createCommonOptions(Composite parent) {
		commonOptions = new Composite(parent, SWT.NULL);
		UI.gridLayout(commonOptions, 1, 10, 0);
		addRegioAndCostChecks(commonOptions);

		Button dqCheck = new Button(commonOptions, SWT.CHECK);
		dqCheck.setText(M.AssessDataQuality);
		dqCheck.setSelection(setup.dqSetup != null);
		Controls.onSelect(dqCheck, e -> {
			boolean selected = dqCheck.getSelection();
			if (selected) {
				setup.initDQSetup();
			} else {
				setup.dqSetup = null;
			}
			dqAssessmentChanged();
		});

		if (Database.isConnected()) {
			Button invenentoryCheck = new Button(commonOptions, SWT.CHECK);
			invenentoryCheck.setSelection(setup.storeInventory);
			invenentoryCheck.setText(M.StoreInventoryResult);
			Controls.onSelect(invenentoryCheck, _e -> {
				setup.storeInventory = invenentoryCheck.getSelection();
			});
		}
	}

	private void createMonteCarloOptions(Composite parent) {
		monteCarloOptions = new Composite(parent, SWT.NONE);
		UI.gridLayout(monteCarloOptions, 1, 10, 0);
		addRegioAndCostChecks(monteCarloOptions);

		// number of iterations
		Composite inner = new Composite(monteCarloOptions, SWT.NONE);
		UI.gridLayout(inner, 2, 10, 0);
		Label label = UI.formLabel(inner, M.NumberOfIterations);
		UI.gridData(label, false, false);
		Text iterText = new Text(inner, SWT.BORDER);
		UI.gridData(iterText, false, false).widthHint = 80;
		String itCount = Preferences.get("calc.numberOfRuns");
		if (Strings.isNullOrEmpty(itCount)) {
			itCount = "100";
		}
		iterText.setText(itCount);
		iterText.addModifyListener(_e -> {
			String text = iterText.getText();
			try {
				setup.calcSetup.numberOfRuns = Integer.parseInt(text);
			} catch (Exception e) {
				MsgBox.error(M.InvalidNumber, text + " " + M.IsNotValidNumber);
			}
		});

	}

	private void addRegioAndCostChecks(Composite comp) {
		Button regioCheck = new Button(comp, SWT.CHECK);
		regioCheck.setText("Regionalized calculation");
		regioCheck.setSelection(setup.calcSetup.withRegionalization);
		Controls.onSelect(regioCheck, _e -> {
			setup.calcSetup.withRegionalization = regioCheck.getSelection();
		});

		Button costCheck = new Button(comp, SWT.CHECK);
		costCheck.setText(M.IncludeCostCalculation);
		costCheck.setSelection(setup.calcSetup.withCosts);
		Controls.onSelect(costCheck, _e -> {
			setup.calcSetup.withCosts = costCheck.getSelection();
		});
	}

	private String getLabel(CalculationType type) {
		switch (type) {
		case UPSTREAM_ANALYSIS:
			return M.Analysis;
		case MONTE_CARLO_SIMULATION:
			return M.MonteCarloSimulation;
		case CONTRIBUTION_ANALYSIS:
			return M.QuickResults;
		default:
			return M.Unknown;
		}
	}

	private void createAllocationCombo(Composite comp) {
		UI.formLabel(comp, M.AllocationMethod);
		AllocationMethodViewer combo = new AllocationMethodViewer(
				comp, AllocationMethod.values());
		combo.setNullable(false);
		if (setup.calcSetup.allocationMethod == null) {
			combo.select(AllocationMethod.NONE);
		} else {
			combo.select(setup.calcSetup.allocationMethod);
		}
		combo.addSelectionChangedListener(m -> {
			setup.calcSetup.allocationMethod = m;
		});
	}

	private void createMethodCombo(Composite comp) {
		UI.formLabel(comp, M.ImpactAssessmentMethod);
		ImpactMethodViewer combo = new ImpactMethodViewer(comp);
		combo.setNullable(true);
		combo.setInput(Database.get());
		combo.select(setup.calcSetup.impactMethod);
		combo.addSelectionChangedListener(_e -> {
			ImpactMethodDescriptor m = combo.getSelected();
			setup.calcSetup.impactMethod = m;
			nwViewer.setInput(m);
		});
	}


	private void dqAssessmentChanged() {
		if (getControl().isVisible()) {
			getContainer().updateButtons();
		}
	}

	@Override
	public boolean canFlipToNextPage() {
		if (!isPageComplete())
			return false;
		return setup.dqSetup != null;
	}

}
