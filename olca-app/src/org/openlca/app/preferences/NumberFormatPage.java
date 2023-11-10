package org.openlca.app.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openlca.app.M;
import org.openlca.app.editors.DataBinding;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NumberFormatPage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private int accuracy;
	private boolean applyFormat;
	private Label sampleLabel;
	private Text numberText;
	private Button applyFormatCheck;
	private double sampleVal = Math.PI * 1000;

	public void setAccuracy(int accuracy) {
		this.accuracy = accuracy;
		setSampleLabel();
	}

	public int getAccuracy() {
		return accuracy;
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Preferences.getStore());
		accuracy = Preferences.getStore().getInt(Preferences.NUMBER_ACCURACY);
		applyFormat = Preferences.getStore().getBoolean(Preferences.FORMAT_INPUT_VALUES);
	}

	@Override
	protected Control createContents(Composite body) {
		var comp = UI.composite(body);
		UI.gridLayout(comp, 2);

		Label description = new Label(comp, SWT.NONE);
		description.setText(M.NumberFormatPage_Description);
		UI.gridData(description, false, false).horizontalSpan = 2;

		createNumberText(comp);
		createExample(comp);
		createApplyFormatCheck(comp);

		return comp;
	}

	private void createApplyFormatCheck(Composite comp) {
		var label = new Label(comp, SWT.NONE);
		label.setText(M.ApplyFormat);
		var gd = UI.gridData(label, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;

		applyFormatCheck = new Button(comp, SWT.CHECK);
		applyFormatCheck.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		new DataBinding().onBoolean(() -> this, "applyFormat", applyFormatCheck);
	}

	private void createNumberText(Composite parent) {
		var label = UI.label(parent, M.NumberOfDecimalPlaces);
		var gd = UI.gridData(label, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;

		numberText = new Text(parent, SWT.BORDER);
		UI.fillHorizontal(numberText);
		UI.gridData(numberText, false, false).widthHint = 80;
		new DataBinding().onInt(() -> this, "accuracy", numberText);
		numberText.addModifyListener((e) -> setSampleLabel());
	}

	private void createExample(Composite parent) {
		Label exampleLabel = new Label(parent, SWT.NONE);
		exampleLabel.setText(M.Example);
		UI.gridData(exampleLabel, false, false).horizontalSpan = 2;
		new Label(parent, SWT.NONE).setText(Double.toString(sampleVal) + " ->");
		sampleLabel = new Label(parent, SWT.NONE);
		UI.gridData(sampleLabel, true, false);
		setSampleLabel();
	}

	private void setSampleLabel() {
		String result = Numbers.format(sampleVal, accuracy);
		if (sampleLabel != null)
			sampleLabel.setText(result);
	}

	@Override
	public boolean performOk() {
		performSave();
		return true;
	}

	@Override
	protected void performDefaults() {
		log.trace("set number of decimal places to default");
		accuracy = Preferences.getStore().getDefaultInt(
				Preferences.NUMBER_ACCURACY);
		Preferences.getStore().setValue(Preferences.NUMBER_ACCURACY, accuracy);
		numberText.setText(Integer.toString(accuracy));
		Numbers.setDefaultAccuracy(accuracy);
		applyFormat = Preferences.getStore().getDefaultBoolean(Preferences.FORMAT_INPUT_VALUES);
		applyFormatCheck.setSelection(applyFormat);
		Preferences.getStore().setValue(Preferences.FORMAT_INPUT_VALUES, applyFormat);
		setSampleLabel();
	}

	@Override
	protected void performApply() {
		performSave();
	}

	private void performSave() {
		log.trace("save number of decimal places to {}", accuracy);
		Numbers.setDefaultAccuracy(accuracy);
		Preferences.getStore().setValue(Preferences.NUMBER_ACCURACY, accuracy);
		Preferences.getStore().setValue(Preferences.FORMAT_INPUT_VALUES, applyFormat);
	}

}
