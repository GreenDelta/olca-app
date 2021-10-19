package org.openlca.app.editors.results;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ModelLink;
import org.openlca.app.editors.results.openepd.output.ExportDialog;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationCombo;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Unit;
import org.openlca.util.Pair;
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
		var unitCombo = new UnitCombo();

		// product system
		UI.formLabel(comp, tk, M.ProductSystem);
		ModelLink.of(ProductSystem.class)
			.setModel(setup().productSystem())
			.renderOn(comp, tk)
			.onChange(system -> {
				setup().withTarget(system);
				flowLink.update();
				unitCombo.update();
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

		// flow
		UI.formLabel(comp, tk, M.Flow);
		flowLink.renderOn(comp, tk).update();

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

		// amount & unit
		UI.formLabel(comp, tk, M.Amount);
		var amountComp = tk.createComposite(comp);
		UI.gridData(amountComp, true, false);
		UI.gridLayout(amountComp, 2, 5, 0);
		var amountStr = Numbers.format(setup().amount());
		var amountText = tk.createText(amountComp, amountStr);
		UI.gridData(amountText, false, false).widthHint = 120;
		amountText.addModifyListener($ -> {
			try {
				var text = amountText.getText();
				var amount = Strings.notEmpty(text)
					? Double.parseDouble(text)
					: 0;
				setup().withAmount(amount);
			} catch (NumberFormatException e) {
				setup().withAmount(0);
			}
			editor.setDirty();
		});
		unitCombo.renderOn(amountComp, tk).update();

		// calculation time
		UI.formLabel(comp, tk, "Calculated at");
		var calcTime = editor.getModel().calculationTime;
		UI.formLabel(comp, tk, Numbers.asTimestamp(calcTime));

		// buttons
		UI.filler(comp, tk);
		var btnComp = tk.createComposite(comp);
		UI.gridLayout(btnComp, 2, 5, 0);
		var calcBtn = tk.createButton(btnComp, "Recalculate", SWT.PUSH);
		calcBtn.setImage(Icon.RUN.get());
		calcBtn.setEnabled(false);
		var expBtn = tk.createButton(btnComp, "Export as EPD", SWT.PUSH);
		expBtn.setImage(Icon.BUILDING.get());
		Controls.onSelect(expBtn, $ -> ExportDialog.show(editor.getModel()));
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

	private class UnitCombo {

		private final List<Pair<Unit, FlowPropertyFactor>> units = new ArrayList<>();
		private Combo combo;

		UnitCombo renderOn(Composite comp, FormToolkit tk) {
			combo = new Combo(comp, SWT.BORDER);
			tk.adapt(combo);
			UI.gridData(combo, false, false).widthHint = 80;

			Controls.onSelect(combo, $ -> {
				var idx = combo.getSelectionIndex();
				if (idx < 0)
					return;
				var pair = units.get(idx);
				setup().withUnit(pair.first);
				setup().withFlowPropertyFactor(pair.second);
				editor.setDirty();
			});
			return this;
		}

		void update() {
			if (combo == null)
				return;

			var flow = setup().flow();
			updateUnits(flow);
			if (units.isEmpty()) {
				combo.setItems();
				return;
			}

			var multiProp = flow.flowPropertyFactors.size() > 1;
			var selectedUnit = setup().unit();
			var selectedFact = setup().flowPropertyFactor();
			var items = new String[units.size()];
			var selectedIdx = -1;

			for (int i = 0; i < units.size(); i++) {
				var pair = units.get(i);
				var unit = pair.first;
				var fact = pair.second;
				var item = Labels.name(unit);
				if (multiProp) {
					item += " (" + Labels.name(fact.flowProperty) + ")";
				}
				items[i] = item;
				if (Objects.equals(unit, selectedUnit)
					&& Objects.equals(fact, selectedFact)) {
					selectedIdx = i;
				}
			}

			combo.setItems(items);
			if (selectedIdx >= 0) {
				combo.select(selectedIdx);
			}
		}

		private void updateUnits(Flow flow) {
			units.clear();
			if (flow == null)
				return;

			for (var fact : flow.flowPropertyFactors) {
				var prop = fact.flowProperty;
				if (prop == null || prop.unitGroup == null)
					continue;
				for (var unit : prop.unitGroup.units) {
					units.add(Pair.of(unit, fact));
				}
			}

			units.sort((pair1, pair2) -> {
				var u1 = Labels.name(pair1.first);
				var u2 = Labels.name(pair2.first);
				var c = Strings.compare(u1, u2);
				if (c != 0) {
					return c;
				}
				var p1 = Labels.name(pair1.second.flowProperty);
				var p2 = Labels.name(pair2.second.flowProperty);
				return Strings.compare(p1, p2);
			});
		}
	}
}
