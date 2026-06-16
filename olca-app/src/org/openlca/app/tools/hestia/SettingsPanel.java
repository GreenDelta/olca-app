package org.openlca.app.tools.hestia;

import java.util.List;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.io.hestia.Release;

class SettingsPanel {

	private final List<Release> releases;
	private final Button aggCheck;
	private final Spinner countSpinner;
	private final Combo versionCombo;

	SettingsPanel(Composite parent, FormToolkit tk, List<Release> releases) {
		this.releases = releases;
		var comp = UI.composite(parent, tk);
		UI.gridLayout(comp, 8, 15, 0);

		aggCheck = UI.checkbox(comp, tk, "Search aggregated data sets");
		aggCheck.setSelection(true);

		UI.label(comp, tk, "|");
		UI.label(comp, tk, "Number of results");
		countSpinner = UI.spinner(comp, tk);
		countSpinner.setValues(25, 5, 500, 0, 5, 15);

		UI.label(comp, tk, "|");
		versionCombo = UI.labeledCombo(comp, tk, "Release");

		// sort highest versions first
		var items = releases.stream()
			.map(r -> r.name() != null ? r.name() : r.version())
			.sorted((v1, v2) -> Strings.compareNatural(v2, v1))
			.toArray(String[]::new);
		versionCombo.setItems(items);
		versionCombo.select(0);
	}

	boolean searchAggregated() {
		return aggCheck.getSelection();
	}

	int numberOfResults() {
		return countSpinner.getSelection();
	}

	String dataVersion() {
		int idx = versionCombo.getSelectionIndex();
		return idx < 0 || idx >= releases.size()
			? null
			: releases.get(idx).version();
	}
}
