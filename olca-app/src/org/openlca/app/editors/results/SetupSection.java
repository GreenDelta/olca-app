package org.openlca.app.editors.results;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ModelLink;
import org.openlca.app.util.UI;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ProductSystem;

class SetupSection {

	private final ResultEditor editor;

	SetupSection(ResultEditor editor) {
		this.editor = editor;
	}

	private CalculationSetup setup() {
		var result = editor.getModel();
		if (result.setup == null) {
			result.setup = new CalculationSetup();
		}
		return result.setup;
	}

	void render(Composite parent, FormToolkit tk) {
		var comp = UI.formSection(parent, tk, "Calculation setup");

		// product system
		UI.formLabel(comp, tk, M.ProductSystem);
		ModelLink.of(ProductSystem.class)
			.setModel(setup().productSystem())
			.renderOn(comp, tk)
			.onChange(system -> {
				// TODO: set product system
				editor.setDirty();
			});

		// impact method
		UI.formLabel(comp, tk, M.ImpactAssessmentMethod);
		ModelLink.of(ImpactMethod.class)
			.setModel(setup().impactMethod())
			.renderOn(comp, tk)
			.onChange(method -> {
				setup().withImpactMethod(method);
				editor.setDirty();
			});


	}

}
