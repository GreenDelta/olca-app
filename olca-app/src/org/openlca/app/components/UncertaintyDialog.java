package org.openlca.app.components;

import java.util.Optional;

import javax.annotation.Nullable;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog for editing uncertainty information.
 */
public class UncertaintyDialog extends Dialog {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final FormToolkit toolkit;
	private Combo combo;
	private StackLayout stackLayout;

	private UncertaintyPanel[] panels;
	private UncertaintyPanel selectedPanel;

	private Uncertainty uncertainty;
	private double defaultMean;

	private UncertaintyDialog(Shell shell, Uncertainty initial) {
		super(shell);
		toolkit = new FormToolkit(shell.getDisplay());
		this.uncertainty = initial == null
			? Uncertainty.none(1.0)
			: initial.copy();
		if (uncertainty.parameter1 != null) {
			defaultMean = uncertainty.parameter1;
		}
	}

	/**
	 * Opens the uncertainty dialog. The given value is used to
	 * initialize the dialog but is not modified. A new uncertainty
	 * instance is returned if the user select an uncertainty
	 * distribution. An empty option is returned if the dialog
	 * is cancelled. An uncertainty distribution with type `None`
	 * is returned when the user selects this option.
	 */
	public static Optional<Uncertainty> open(@Nullable Uncertainty init) {
		var dialog = new UncertaintyDialog(UI.shell(), init);
		if (dialog.open() != OK
				|| dialog.uncertainty == null
				|| dialog.uncertainty.distributionType == null)
			return Optional.empty();
		return Optional.of(dialog.uncertainty);
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
		uncertainty = selectedPanel.fetchUncertainty();
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
		Uncertainty u = selectedPanel.fetchUncertainty();
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
		var types = UncertaintyType.values();
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
		selectedPanel = panels[item];
		stackLayout.topControl = selectedPanel.composite;
		getShell().layout(true, true);
		getShell().pack();
	}

	private void createCompositeStack(Composite parent) {
		Composite stack = toolkit.createComposite(parent);
		UI.gridData(stack, true, true);
		stackLayout = new StackLayout();
		stack.setLayout(stackLayout);
		var types = UncertaintyType.values();
		panels = new UncertaintyPanel[types.length];
		for (int i = 0; i < types.length; i++) {
			Composite comp = toolkit.createComposite(stack);
			UI.gridLayout(comp, 2);
			UncertaintyPanel client = new UncertaintyPanel(comp, types[i]);
			panels[i] = client;
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

		private final Composite composite;
		private final Uncertainty _uncertainty;
		private Text[] texts;

		UncertaintyPanel(Composite composite, UncertaintyType type) {
			this.composite = composite;
			_uncertainty = type == uncertainty.distributionType
				? uncertainty
				: createUncertainty(type);
			if (type != UncertaintyType.NONE)
				createTextFields();
			else {
				toolkit.createLabel(composite, M.NoDistribution)
					.setForeground(Colors.darkGray());
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
					return new String[]{M.GeometricMean, M.GeometricStandardDeviation};
				case NORMAL:
					return new String[]{M.Mean, M.StandardDeviation};
				case TRIANGLE:
					return new String[]{M.Minimum, M.Mode, M.Maximum};
				case UNIFORM:
					return new String[]{M.Minimum, M.Maximum};
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
				case NORMAL:
				case UNIFORM:
					return parameter == 1 || parameter == 2;
				case TRIANGLE:
					return parameter == 1 || parameter == 2 || parameter == 3;
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
					set(param, Double.parseDouble(s));
				} catch (Exception e) {
					MsgBox.error(s + " " + M.IsNotValidNumber);
				}
			}
			return _uncertainty;
		}

		private void set(int param, double val) {
			switch (param) {
				case 1:
					_uncertainty.parameter1 = val;
					break;
				case 2:
					_uncertainty.parameter2 = val;
					break;
				case 3:
					_uncertainty.parameter3 = val;
					break;
				default:
					break;
			}
		}
	}
}
