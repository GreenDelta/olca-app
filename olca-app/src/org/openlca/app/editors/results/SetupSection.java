package org.openlca.app.editors.results;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ModelLink;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationCombo;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ProductSystem;
import org.openlca.util.Strings;

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
		var comp = UI.formSection(parent, tk, "Calculation setup", 2);
		UI.gridData(comp, false, false);
		var flowLink = new FlowLink();

		// product system
		UI.formLabel(comp, tk, M.ProductSystem);
		ModelLink.of(ProductSystem.class)
			.setModel(setup().productSystem())
			.renderOn(comp, tk)
			.onChange(system -> {
				setup().withTarget(system);
				flowLink.update();
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

		// flow
		UI.formLabel(comp, tk, M.Flow);
		flowLink.renderOn(comp, tk).update();

	}

	private class FlowLink {

		private ImageHyperlink link;

		private FlowLink renderOn(Composite parent, FormToolkit tk) {
			var comp = tk.createComposite(parent, SWT.FILL);
			UI.gridLayout(comp, 1, 0, 0);
			link = tk.createImageHyperlink(comp, SWT.TOP);
			link.setForeground(Colors.linkBlue());
			Controls.onClick(link, $ -> {
				var flow = setup().flow();
				if (flow != null) {
					App.open(flow);
				}
			});
			return this;
		}

		void update() {
			if (link == null)
				return;
			var flow = setup().flow();
			var text = flow == null
				? M.None
				: Labels.name(flow);
			link.setText(Strings.cut(text, 120));
			link.setToolTipText(text);
			link.getParent().pack();
			link.setEnabled(flow != null);
		}
	}
}
