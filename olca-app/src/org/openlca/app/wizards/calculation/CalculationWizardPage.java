package org.openlca.app.wizards.calculation;

import java.util.ArrayList;
import java.util.Objects;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationCombo;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.combo.NwSetComboViewer;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Strings;

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
	public boolean canFlipToNextPage() {
		if (!isPageComplete())
			return false;
		return setup.withDataQuality
				&& setup.type != CalculationType.SIMULATION;
	}

	@Override
	public void createControl(Composite parent) {
		Composite body = UI.formComposite(parent);
		UI.gridLayout(body, 2, 10, 10);
		setControl(body);

		// main selectors
		createParamSetCombo(body);
		createAllocationCombo(body);
		createMethodCombo(body);
		createNWSetCombo(body);
		createTypeRadios(body);

		// separator
		UI.formLabel(body, null);
		UI.gridData(new Label(
						body, SWT.SEPARATOR | SWT.HORIZONTAL),
				true, false);
		UI.formLabel(body);

		// options
		optionStack = UI.formComposite(body);
		var optionsLayout = new StackLayout();
		optionStack.setLayout(optionsLayout);
		createMonteCarloOptions(optionStack);
		createCommonOptions(optionStack);
		optionsLayout.topControl = commonOptions;
		UI.formLabel(body);

		updateOptions();
	}

	private void createParamSetCombo(Composite comp) {
		if (!setup.calcSetup.hasProductSystem())
			return;
		var paramSets = new ArrayList<>(
				setup.calcSetup.productSystem().parameterSets);
		if (paramSets.size() < 2)
			return;

		paramSets.sort((s1, s2) -> {
			if (s1.isBaseline)
				return -1;
			if (s2.isBaseline)
				return 1;
			return Strings.compare(s1.name, s2.name);
		});

		UI.formLabel(comp, "Parameter set");
		var combo = UI.formTableCombo(comp, null,
				SWT.READ_ONLY | SWT.BORDER);
		UI.gridData(combo, true, false);

		for (var paramSet : paramSets) {
			var item = new TableItem(
					combo.getTable(), SWT.NONE);
			item.setText(paramSet.name);
		}

		combo.select(0);
		setup.setParameters(paramSets.get(0));
		Controls.onSelect(combo, e -> {
			int i = combo.getSelectionIndex();
			setup.setParameters(paramSets.get(i));
		});
	}

	private void createAllocationCombo(Composite comp) {
		UI.formLabel(comp, M.AllocationMethod);
		var combo = new AllocationCombo(
				comp, AllocationMethod.values());
		combo.setNullable(false);
		combo.select(Objects.requireNonNullElse(
				setup.calcSetup.allocation(), AllocationMethod.NONE));
		combo.addSelectionChangedListener(setup.calcSetup::withAllocation);
	}

	private void createMethodCombo(Composite comp) {
		UI.formLabel(comp, M.ImpactAssessmentMethod);
		var combo = new ImpactMethodViewer(comp);
		combo.setNullable(true);
		combo.setInput(Database.get());
		if (setup.calcSetup.impactMethod() != null) {
			combo.select(Descriptor.of(setup.calcSetup.impactMethod()));
		}
		combo.addSelectionChangedListener(_e -> {
			var method = combo.getSelected();
			nwViewer.setInput(method);
			setup.setMethod(method);
		});
	}

	private void createNWSetCombo(Composite parent) {
		UI.formLabel(parent, M.NormalizationAndWeightingSet);
		nwViewer = new NwSetComboViewer(parent, Database.get());
		nwViewer.setNullable(true);
		var method = setup.calcSetup.impactMethod();
		if (method != null) {
			nwViewer.setInput(Descriptor.of(method));
		}
		if (setup.calcSetup.nwSet() != null) {
			nwViewer.select(setup.calcSetup.nwSet());
		}
		nwViewer.addSelectionChangedListener(setup::setNwSet);
	}

	private void createTypeRadios(Composite parent) {
		CalculationType[] types = {
				CalculationType.LAZY,
				CalculationType.EAGER,
				CalculationType.SIMULATION,
		};
		boolean[] enabled = {
				true,
				true,
				!setup.hasLibraries,
		};

		UI.formLabel(parent, M.CalculationType);
		Composite comp = UI.formComposite(parent, SWT.NO_RADIO_GROUP);
		UI.gridLayout(comp, types.length, 10, 0);

		var radios = new Button[types.length];
		for (int i = 0; i < types.length; i++) {
			var radio = UI.formRadio(comp);
			radio.setText(getLabel(types[i]));
			radio.setSelection(setup.type == types[i]);
			radio.setEnabled(enabled[i]);
			radios[i] = radio;
			Controls.onSelect(radio, e -> {
				for (int j = 0; j < types.length; j++) {
					if (radios[j] == radio) {
						radio.setSelection(true);
						setup.type = types[j];
					} else {
						radios[j].setSelection(false);
					}
				}
				updateOptions();
			});
		}
	}

	private String getLabel(CalculationType type) {
		return switch (type) {
			case EAGER -> "Eager/All";
			case SIMULATION -> M.MonteCarloSimulation;
			case LAZY -> "Lazy/On-demand";
		};
	}

	private void createCommonOptions(Composite parent) {
		commonOptions = UI.formComposite(parent);
		UI.gridLayout(commonOptions, 1, 10, 0);
		addRegioAndCostChecks(commonOptions);

		var dqCheck = UI.checkBox(commonOptions, M.AssessDataQuality);
		dqCheck.setSelection(setup.withDataQuality);
		Controls.onSelect(dqCheck, e -> {
			setup.withDataQuality = dqCheck.getSelection();
			getContainer().updateButtons();
		});
		if (setup.hasLibraries) {
			dqCheck.setEnabled(false);
		}
	}

	private void createMonteCarloOptions(Composite parent) {
		monteCarloOptions = UI.formComposite(parent);
		UI.gridLayout(monteCarloOptions, 1, 10, 0);
		addRegioAndCostChecks(monteCarloOptions);

		// number of iterations
		var inner = UI.formComposite(monteCarloOptions);
		UI.gridLayout(inner, 2, 10, 0);
		var label = UI.formLabel(inner, M.NumberOfIterations);
		UI.gridData(label, false, false);
		var countText = UI.formText(inner, SWT.BORDER);
		UI.gridData(countText, false, false).widthHint = 80;

		countText.setText(Integer.toString(setup.simulationRuns));
		countText.addModifyListener(_e -> {
			var count = countText.getText();
			try {
				setup.simulationRuns = Integer.parseInt(count);
			} catch (Exception e) {
				MsgBox.error(M.InvalidNumber, count + " " + M.IsNotValidNumber);
			}
		});
	}

	private void addRegioAndCostChecks(Composite comp) {
		var regioCheck = UI.checkBox(comp, "Regionalized calculation");
		regioCheck.setSelection(setup.calcSetup.hasRegionalization());
		Controls.onSelect(regioCheck,
				_e -> setup.calcSetup.withRegionalization(regioCheck.getSelection()));

		var costCheck = UI.checkBox(comp, M.IncludeCostCalculation);
		costCheck.setSelection(setup.calcSetup.hasCosts());
		Controls.onSelect(costCheck,
				_e -> setup.calcSetup.withCosts(costCheck.getSelection()));
		if (setup.hasLibraries) {
			costCheck.setEnabled(false);
		}
	}

	private void updateOptions() {
		var layout = (StackLayout) optionStack.getLayout();
		layout.topControl = setup.type == CalculationType.SIMULATION
				? monteCarloOptions
				: commonOptions;
		optionStack.layout();
		getContainer().updateButtons();
	}

}
