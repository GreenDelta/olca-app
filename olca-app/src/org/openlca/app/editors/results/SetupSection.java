package org.openlca.app.editors.results;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ModelLink;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationCombo;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ProductSystem;

record SetupSection(ResultEditor editor) {

	private CalculationSetup setup() {
		var result = editor.getModel();
		if (result.setup == null) {
			result.setup = new CalculationSetup();
		}
		return result.setup;
	}

	void render(Composite parent, FormToolkit tk) {
		var comp = UI.formSection(parent, tk, "Calculation setup", 2);
		UI.gridData(comp, false, false);

		// product system
		UI.formLabel(comp, tk, M.ProductSystem);
		ModelLink.of(ProductSystem.class)
			.setModel(setup().productSystem())
			.renderOn(comp, tk)
			.onChange(system -> {
				setup().withTarget(system);
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

		// allocation
		UI.formLabel(comp, tk, M.AllocationMethod);
		var allocationCombo = new AllocationCombo(comp,
			AllocationMethod.USE_DEFAULT,
			AllocationMethod.PHYSICAL,
			AllocationMethod.ECONOMIC,
			AllocationMethod.CAUSAL,
			AllocationMethod.NONE);
		allocationCombo.select(setup().allocation());
		allocationCombo.addSelectionChangedListener(allocation -> {
			setup().withAllocation(allocation);
			editor.setDirty();
		});
		var allocControl = allocationCombo.getViewer().getControl();
		UI.gridData(allocControl, false, false).widthHint = 250;

		// calculation time
		UI.formLabel(comp, tk, "Calculated at");
		var calcTime = editor.getModel().calculationTime;
		UI.formLabel(comp, tk, Numbers.asTimestamp(calcTime));

		// calculation button
		UI.filler(comp, tk);
		var calcBtn = tk.createButton(comp, "Recalculate", SWT.PUSH);
		calcBtn.setImage(Icon.RUN.get());
		calcBtn.setEnabled(false);
	}
}
