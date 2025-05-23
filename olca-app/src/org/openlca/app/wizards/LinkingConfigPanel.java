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
import org.openlca.core.matrix.linking.LinkingConfig;
import org.openlca.core.matrix.linking.LinkingConfig.PreferredType;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.ProcessType;

import com.google.common.base.Strings;

class LinkingConfigPanel {

	private Button ignoreProvidersRadio;
	private Button preferProvidersRadio;
	private Button onlyLinkProvidersRadio;

	private Button preferUnitRadio;
	private Button preferSystemRadio;

	private Button cutoffCheck;
	private Text cutoffText;

	LinkingConfigPanel(Composite comp) {
		create(comp);
	}

	private void create(Composite comp) {
		Composite methodGroup = createRadioGroup(comp, M.ProviderLinking);
		ignoreProvidersRadio = UI.labeledRadio(methodGroup, M.IgnoreDefaultProviders);
		preferProvidersRadio = UI.labeledRadio(methodGroup, M.PreferDefaultProviders);
		onlyLinkProvidersRadio = UI.labeledRadio(methodGroup, M.OnlyLinkDefaultProviders);
		Composite typeGroup = createRadioGroup(comp, M.PreferredProcessType);
		preferUnitRadio = UI.labeledRadio(typeGroup, Labels.of(ProcessType.UNIT_PROCESS));
		preferSystemRadio = UI.labeledRadio(typeGroup, Labels.of(ProcessType.LCI_RESULT));
		createCutoffText(comp);
		preferProvidersRadio.setSelection(true);
		preferSystemRadio.setSelection(true);
		cutoffText.setEnabled(false);
		initializeChangeHandler();
	}

	private Composite createRadioGroup(Composite parent, String label) {
		UI.filler(parent);
		UI.label(parent, label);
		UI.filler(parent);
		Composite group = UI.composite(parent);
		UI.gridLayout(group, 2, 10, 0).marginLeft = 10;
		return group;
	}

	private void createCutoffText(Composite comp) {
		UI.filler(comp);
		var inner = UI.composite(comp);
		UI.fillHorizontal(inner);
		UI.gridLayout(inner, 2, 5, 0);
		cutoffCheck = UI.checkbox(inner, M.Cutoff);
		cutoffText = UI.emptyText(inner, SWT.BORDER);
		UI.gridData(cutoffText, false, false).widthHint = 100;
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
		var config = new LinkingConfig();
		if (preferUnitRadio.getSelection()) {
			config.preferredType(PreferredType.UNIT_PROCESS);
		} else {
			config.preferredType(PreferredType.SYSTEM_PROCESS);
		}
		if (ignoreProvidersRadio.getSelection()) {
			config.providerLinking(ProviderLinking.IGNORE_DEFAULTS);
		} else if (onlyLinkProvidersRadio.getSelection()) {
			config.providerLinking(ProviderLinking.ONLY_DEFAULTS);
		} else {
			config.providerLinking(ProviderLinking.PREFER_DEFAULTS);
		}
		config.cutoff(getCutoff());
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
