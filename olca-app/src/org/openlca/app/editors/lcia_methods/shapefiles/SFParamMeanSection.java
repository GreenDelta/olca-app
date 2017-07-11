package org.openlca.app.editors.lcia_methods.shapefiles;

import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ImpactMethod.ParameterMean;

class SFParamMeanSection {

	private final ShapeFilePage page;

	public SFParamMeanSection(ShapeFilePage page) {
		this.page = page;
	}

	void render(Composite body, FormToolkit tk) {
		Composite comp = UI.formSection(body, tk,
				"#Parameter aggregation function");
		Combo combo = UI.formCombo(comp, tk, "#Function");
		UI.gridData(combo, false, false).widthHint = 160;
		combo.setItems(new String[] {
				"#Weighted arithmetic mean",
				"#Arithmetic mean"
		});
		ImpactMethod m = page.editor.getModel();
		int idx = m.parameterMean == ParameterMean.ARITHMETIC_MEAN ? 1 : 0;
		combo.select(idx);
		onChange(combo);
	}

	private void onChange(Combo combo) {
		Controls.onSelect(combo, e -> {
			ParameterMean fn = combo.getSelectionIndex() == 0
					? ParameterMean.WEIGHTED_MEAN
					: ParameterMean.ARITHMETIC_MEAN;
			page.editor.getModel().parameterMean = fn;
			page.editor.setDirty(true);
		});
	}
}
