package org.openlca.app.editors.processes.social;

import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.RiskLevel;
import org.openlca.core.model.SocialAspect;

class RiskCombo {

	private SocialAspect aspect;
	private Combo combo;
	private RiskLevel[] levels;

	RiskCombo(SocialAspect aspect) {
		this.aspect = aspect;
	}

	void create(Composite body, FormToolkit tk) {
		combo = UI.formCombo(body, tk, M.RiskLevel);
		levels = RiskLevel.values();
		String[] labels = new String[levels.length];
		int selected = -1;
		for (int i = 0; i < levels.length; i++) {
			labels[i] = Labels.of(levels[i]);
			if (aspect.riskLevel == levels[i])
				selected = i;
		}
		combo.setItems(labels);
		if (selected >= 0)
			combo.select(selected);
		Controls.onSelect(combo, (e) -> selectionChanged());
		UI.filler(body, tk);
	}

	private void selectionChanged() {
		int i = combo.getSelectionIndex();
		if (i < 0) {
			aspect.riskLevel = null;
		} else {
			aspect.riskLevel = levels[i];
		}
	}
}
