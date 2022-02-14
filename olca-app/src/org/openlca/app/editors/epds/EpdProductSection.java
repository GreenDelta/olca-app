package org.openlca.app.editors.epds;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ModelLink;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.FlowPropertyCombo;
import org.openlca.app.viewers.combo.UnitCombo;
import org.openlca.core.model.EpdProduct;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Unit;

import java.util.Objects;

public record EpdProductSection(EpdEditor editor) {

	void render(Composite body, FormToolkit tk) {

		var comp = UI.formSection(body, tk, "Declared product");
		var flowLink = ModelLink.of(Flow.class)
			.renderOn(comp, tk, M.Flow)
			.setModel(product().flow);

		// flow properties
		UI.formLabel(comp, tk, M.FlowProperty);
		var propCombo = new FlowPropertyCombo(comp);
		fillProperties(propCombo);
		if (product().property !=  null) {
			propCombo.select(product().property);
		}
		propCombo.addSelectionChangedListener(property -> {
			product().property = property;
			editor.setDirty();
		});

		// units
		UI.formLabel(comp, tk, M.Unit);
		var unitCombo = new UnitCombo(comp);
		fillUnits(unitCombo);
		if (product().unit != null) {
			unitCombo.select(product().unit);
		}
		unitCombo.addSelectionChangedListener(unit -> {
			product().unit = unit;
			editor.setDirty();
		});

		// on flow change
		flowLink.onChange(flow -> {
			var product = product();
			product.flow = flow;
			product.property = flow != null
				? flow.referenceFlowProperty
				: null;
			product.unit = flow != null
				? flow.getReferenceUnit()
				: null;
			fillProperties(propCombo);
			fillUnits(unitCombo);

			propCombo.select(product.property);
			unitCombo.select(product.unit);
			editor.setDirty();
		});

		var amountText = UI.formText(comp, tk, M.Amount);

	}

	private EpdProduct product() {
		var epd = editor.getModel();
		if (epd.product == null) {
			epd.product = new EpdProduct();
		}
		return epd.product;
	}

	private void fillProperties(FlowPropertyCombo combo) {
		var flow = product().flow;
		if (flow == null) {
			combo.setInput(new FlowProperty[0]);
			return;
		}
		var props = flow.flowPropertyFactors.stream()
			.map(f -> f.flowProperty)
			.filter(Objects::nonNull)
			.toArray(FlowProperty[]::new);
		combo.setInput(props);
	}

	private void fillUnits(UnitCombo combo) {
		var prop = product().property;
		if (prop == null || prop.unitGroup == null) {
			combo.setInput(new Unit[0]);
			return;
		}
		combo.setInput(prop.unitGroup);
	}

}
