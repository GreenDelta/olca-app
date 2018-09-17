package org.openlca.app.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.matrix.LinkingConfig;
import org.openlca.core.matrix.LinkingConfig.DefaultProviders;
import org.openlca.core.model.ProcessType;

import com.google.common.base.Strings;

public class LinkingConfigPanel {

	private Button ignoreProvidersRadio;
	private Button preferProvidersRadio;
	private Button onlyLinkProvidersRadio;

	private Button preferUnitRadio;
	private Button preferSystemRadio;

	private Button cutoffCheck;
	private Text cutoffText;

	public LinkingConfigPanel(Composite comp) {
		create(comp);
	}

	private void create(Composite comp) {
		Composite methodGroup = createRadioGroup(comp, M.ProviderLinking);
		ignoreProvidersRadio = UI.formRadio(methodGroup, M.IgnoreDefaultProviders);
		preferProvidersRadio = UI.formRadio(methodGroup, M.PreferDefaultProviders);
		onlyLinkProvidersRadio = UI.formRadio(methodGroup, M.OnlyLinkDefaultProviders);
		Composite typeGroup = createRadioGroup(comp, M.PreferredProcessType);
		preferUnitRadio = UI.formRadio(typeGroup, Labels.processType(ProcessType.UNIT_PROCESS));
		preferSystemRadio = UI.formRadio(typeGroup, Labels.processType(ProcessType.LCI_RESULT));
		createCutoffText(comp);
		preferProvidersRadio.setSelection(true);
		preferSystemRadio.setSelection(true);
		cutoffText.setEnabled(false);
		initializeChangeHandler();
	}

	private Composite createRadioGroup(Composite parent, String label) {
		UI.filler(parent);
		UI.formLabel(parent, label);
		UI.filler(parent);
		Composite group = UI.formComposite(parent);
		UI.gridLayout(group, 2, 10, 0).marginLeft = 10;
		return group;
	}

	private void createCutoffText(Composite comp) {
		UI.filler(comp);
		Composite inner = new Composite(comp, SWT.NONE);
		UI.gridLayout(inner, 2, 5, 0);
		cutoffCheck = UI.checkBox(inner, M.Cutoff);
		cutoffText = new Text(inner, SWT.BORDER);
		UI.gridData(cutoffText, true, false);
	}

	private void initializeChangeHandler() {
		Controls.onSelect(ignoreProvidersRadio, this::onLinkingMethodChange);
		Controls.onSelect(preferProvidersRadio, this::onLinkingMethodChange);
		Controls.onSelect(onlyLinkProvidersRadio, this::onLinkingMethodChange);
		Controls.onSelect(cutoffCheck, e -> cutoffText.setEnabled(cutoffCheck.getSelection()));
	}

	private void onLinkingMethodChange(SelectionEvent e) {
		preferUnitRadio.setEnabled(!onlyLinkProvidersRadio.getSelection());
		preferSystemRadio.setEnabled(!onlyLinkProvidersRadio.getSelection());
	}

	public void setEnabled(boolean enabled) {
		preferUnitRadio.setEnabled(enabled);
		preferSystemRadio.setEnabled(enabled);
		ignoreProvidersRadio.setEnabled(enabled);
		preferProvidersRadio.setEnabled(enabled);
		onlyLinkProvidersRadio.setEnabled(enabled);
		cutoffCheck.setEnabled(enabled);
		cutoffText.setEnabled(enabled);
	}

	public void setTypeChecksEnabled(boolean enabled) {
		preferUnitRadio.setEnabled(enabled);
		preferSystemRadio.setEnabled(enabled);
	}

	public LinkingConfig getLinkingConfig() {
		LinkingConfig config = new LinkingConfig();
		if (preferUnitRadio.getSelection()) {
			config.preferredType = ProcessType.UNIT_PROCESS;
		} else {
			config.preferredType = ProcessType.LCI_RESULT;
		}
		if (ignoreProvidersRadio.getSelection()) {
			config.providerLinking = DefaultProviders.IGNORE;
		} else if (onlyLinkProvidersRadio.getSelection()) {
			config.providerLinking = DefaultProviders.ONLY;
		} else {
			config.providerLinking = DefaultProviders.PREFER;
		}
		config.cutoff = getCutoff();
		return config;
	}

	private Double getCutoff() {
		String s = cutoffText.getText();
		if (Strings.isNullOrEmpty(s)) {
			return null;
		}
		try {
			return Double.parseDouble(s);
		} catch (Exception ex) {
			return null;
		}
	}
}
