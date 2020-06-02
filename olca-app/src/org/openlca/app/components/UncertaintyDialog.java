package org.openlca.app.components;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.math.NumberGenerator;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.UncertaintyType;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.expressions.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Dialog for editing uncertainty informations . */
public class UncertaintyDialog extends Dialog {

	private Logger log = LoggerFactory.getLogger(getClass());

	private FormToolkit toolkit;
	private UncertaintyType[] types = UncertaintyType.values();
	private Combo combo;
	private StackLayout stackLayout;

	private UncertaintyPanel[] clients;
	private UncertaintyPanel selectedClient;
	private Scope interpreterScope;

	private Uncertainty uncertainty;
	private double defaultMean;

	public UncertaintyDialog(Shell shell, Uncertainty initial) {
		super(shell);
		toolkit = new FormToolkit(shell.getDisplay());
		if (initial == null)
			initial = new Uncertainty();
		this.uncertainty = initial.clone();
		if (uncertainty.parameter1 != null) {
			defaultMean = uncertainty.parameter1;
		}

	}

	public Uncertainty getUncertainty() {
		return uncertainty;
	}

	public void setInterpreter(FormulaInterpreter interpreter, long scope) {
		if (!interpreter.hasScope(scope))
			this.interpreterScope = interpreter.getGlobalScope();
		else
			this.interpreterScope = interpreter.getScope(scope);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		toolkit.adapt(parent);
		createButton(parent, IDialogConstants.OK_ID, M.OK, true);
		createButton(parent, IDialogConstants.HELP_ID, M.Test, false);
		createButton(parent, IDialogConstants.CANCEL_ID, M.Cancel, false);
		getShell().pack();
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}

	@Override
	protected void okPressed() {
		uncertainty = selectedClient.fetchUncertainty();
		super.okPressed();
	}

	@Override
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		if (buttonId != IDialogConstants.HELP_ID)
			return;
		try {
			UncertaintyShell.show(makeGenerator());
		} catch (Exception e) {
			log.error("failed to run uncertainty test");
		}
	}

	private NumberGenerator makeGenerator() {
		Uncertainty u = selectedClient.fetchUncertainty();
		switch (u.distributionType) {
		case LOG_NORMAL:
			return NumberGenerator.logNormal(
					u.parameter1,
					u.parameter2);
		case NONE:
			return NumberGenerator.discrete(
					u.parameter1);
		case NORMAL:
			return NumberGenerator.normal(
					u.parameter1,
					u.parameter2);
		case TRIANGLE:
			return NumberGenerator.triangular(
					u.parameter1,
					u.parameter2,
					u.parameter3);
		case UNIFORM:
			return NumberGenerator.uniform(
					u.parameter1,
					u.parameter2);
		default:
			return NumberGenerator.discrete(1);
		}
	}

	@Override
	protected Control createDialogArea(Composite root) {
		getShell().setText(M.Uncertainty);
		toolkit.adapt(root);
		Composite area = (Composite) super.createDialogArea(root);
		toolkit.adapt(area);
		Composite comp = toolkit.createComposite(area);
		UI.gridData(comp, true, true);
		UI.gridLayout(comp, 1);
		createCombo(comp);
		createCompositeStack(comp);
		initComposite();
		getShell().pack();
		UI.center(getParentShell(), getShell());
		return area;
	}

	private void createCombo(Composite parent) {
		Composite comp = toolkit.createComposite(parent);
		UI.gridData(comp, true, false);
		UI.gridLayout(comp, 2);
		combo = UI.formCombo(comp, toolkit,
				M.UncertaintyDistribution);
		String[] items = new String[types.length];
		int idx = 0;
		for (int i = 0; i < items.length; i++) {
			UncertaintyType type = types[i];
			items[i] = Labels.of(type);
			if (uncertainty != null
					&& uncertainty.distributionType == type)
				idx = i;
		}
		combo.setItems(items);
		combo.select(idx);
		Controls.onSelect(combo, (e) -> initComposite());
	}

	private void initComposite() {
		int item = combo.getSelectionIndex();
		if (item == -1)
			return;
		selectedClient = clients[item];
		stackLayout.topControl = selectedClient.composite;
		getShell().layout(true, true);
		getShell().pack();
	}

	private void createCompositeStack(Composite parent) {
		Composite stack = toolkit.createComposite(parent);
		UI.gridData(stack, true, true);
		stackLayout = new StackLayout();
		stack.setLayout(stackLayout);
		clients = new UncertaintyPanel[types.length];
		for (int i = 0; i < types.length; i++) {
			Composite comp = toolkit.createComposite(stack);
			UI.gridLayout(comp, 2);
			UncertaintyPanel client = new UncertaintyPanel(comp, types[i]);
			clients[i] = client;
		}
	}

	@Override
	public boolean close() {
		if (toolkit != null) {
			toolkit.dispose();
		}
		return super.close();
	}

	private class UncertaintyPanel {

		private Composite composite;
		private Uncertainty _uncertainty;
		private Text[] texts;

		UncertaintyPanel(Composite composite, UncertaintyType type) {
			this.composite = composite;
			if (type == uncertainty.distributionType)
				_uncertainty = uncertainty;
			else
				_uncertainty = createUncertainty(type);
			if (type != UncertaintyType.NONE)
				createTextFields();
			else {
				Label label = toolkit.createLabel(composite,
						M.NoDistribution);
				label.setForeground(Colors.darkGray());
			}
		}

		private void createTextFields() {
			String[] labels = getLabels();
			texts = new Text[labels.length];
			for (int param = 1; param <= 3; param++) {
				if (!hasParameter(param))
					continue;
				String label = labels[param - 1];
				Text text = UI.formText(composite, toolkit, label);
				text.setText(initialValue(param));
				texts[param - 1] = text;
			}
		}

		private String initialValue(int param) {
			switch (param) {
			case 1:
				return initialValue(_uncertainty.parameter1,
						_uncertainty.formula1);
			case 2:
				return initialValue(_uncertainty.parameter2,
						_uncertainty.formula2);
			case 3:
				return initialValue(_uncertainty.parameter3,
						_uncertainty.formula3);
			default:
				return "";
			}

		}

		private String initialValue(Double value, String formula) {
			if (formula != null && !formula.isEmpty())
				return formula;
			if (value == null)
				return "";
			else
				return Double.toString(value);
		}

		private String[] getLabels() {
			switch (_uncertainty.distributionType) {
			case LOG_NORMAL:
				return new String[] { M.GeometricMean,
						M.GeometricStandardDeviation };
			case NONE:
				return new String[0];
			case NORMAL:
				return new String[] { M.Mean, M.StandardDeviation };
			case TRIANGLE:
				return new String[] { M.Minimum, M.Mode,
						M.Maximum };
			case UNIFORM:
				return new String[] { M.Minimum, M.Maximum };
			default:
				return new String[0];
			}
		}

		private Uncertainty createUncertainty(UncertaintyType type) {
			switch (type) {
			case LOG_NORMAL:
				return Uncertainty.logNormal(defaultMean, 1);
			case NONE:
				return Uncertainty.none(defaultMean);
			case NORMAL:
				return Uncertainty.normal(defaultMean, 1);
			case TRIANGLE:
				return Uncertainty.triangle(defaultMean, defaultMean,
						defaultMean);
			case UNIFORM:
				return Uncertainty.uniform(defaultMean, defaultMean);
			default:
				return null;
			}
		}

		private boolean hasParameter(int parameter) {
			switch (_uncertainty.distributionType) {
			case LOG_NORMAL:
				return parameter == 1 || parameter == 2;
			case NONE:
				return false;
			case NORMAL:
				return parameter == 1 || parameter == 2;
			case TRIANGLE:
				return parameter == 1 || parameter == 2 || parameter == 3;
			case UNIFORM:
				return parameter == 1 || parameter == 2;
			default:
				return false;
			}
		}

		Uncertainty fetchUncertainty() {
			if (texts == null)
				return _uncertainty;
			for (int i = 0; i < texts.length; i++) {
				String s = texts[i].getText();
				int param = i + 1;
				try {
					if (isValidNumber(s))
						set(param, Double.parseDouble(s), null);
					else if (isValidFormula(s))
						set(param, interpreterScope.eval(s), s);
				} catch (Exception e) {
					log.error("failed to set uncertainty value", e);
				}
			}
			return _uncertainty;
		}

		private void set(int param, double val, String s) {
			switch (param) {
			case 1:
				_uncertainty.formula1 = s;
				_uncertainty.parameter1 = val;
				break;
			case 2:
				_uncertainty.formula2 = s;
				_uncertainty.parameter2 = val;
				break;
			case 3:
				_uncertainty.formula3 = s;
				_uncertainty.parameter3 = val;
				break;
			default:
				break;
			}
		}

		private boolean isValidNumber(String s) {
			try {
				Double.parseDouble(s);
				return true;
			} catch (Exception e) {
				if (interpreterScope == null)
					MsgBox.error(s + " " + M.IsNotValidNumber);
				return false;
			}
		}

		private boolean isValidFormula(String s) {
			if (interpreterScope == null)
				return false;
			try {
				interpreterScope.eval(s);
				return true;
			} catch (Exception e) {
				MsgBox.error(M.FormulaEvaluationFailed + ": " + s);
				return false;
			}
		}

	}

}
