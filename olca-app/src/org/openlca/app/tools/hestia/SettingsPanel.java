package org.openlca.app.tools.hestia;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.UI;

class SettingsPanel {

	private final Button aggCheck;
	private final Spinner countSpinner;

	SettingsPanel(Composite parent, FormToolkit tk) {
		var comp = UI.composite(parent, tk);
		UI.gridLayout(comp, 8, 15, 0);

		aggCheck = UI.checkbox(comp, tk, "Search aggregated data sets");
		aggCheck.setSelection(true);

		UI.label(comp, tk, "|");

		UI.label(comp, tk, "Number of results");
		countSpinner = UI.spinner(comp, tk);
		countSpinner.setValues(25, 5, 500, 0, 5, 15);
	}

	boolean searchAggregated() {
		return aggCheck.getSelection();
	}

	int numberOfResults() {
		return countSpinner.getSelection();
	}
}
