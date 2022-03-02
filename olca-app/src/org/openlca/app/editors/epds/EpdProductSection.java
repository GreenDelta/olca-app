package org.openlca.app.editors.epds;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ModelLink;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.model.EpdProduct;
import org.openlca.core.model.Flow;

public record EpdProductSection(EpdEditor editor) {

	void render(Composite body, FormToolkit tk) {

		var comp = UI.formSection(body, tk, "Declared product");
		var flowLink = ModelLink.of(Flow.class)
			.renderOn(comp, tk, M.Flow)
			.setModel(product().flow);

		// amount
		UI.formLabel(comp, M.Amount);
		var amountComp = tk.createComposite(comp);
		UI.gridLayout(amountComp, 2, 5, 0);
		var amountText = tk.createText(amountComp, "", SWT.BORDER);
		UI.gridData(amountText, false, false).widthHint = 100;
		Controls.set(amountText, product().amount, amount -> {
			product().amount = amount;
			editor.setDirty();
		});

		// unit
		var combo = new Combo(amountComp, SWT.READ_ONLY);
		UI.gridData(combo, false, false).widthHint = 50;
		var units = UnitCombo.of(combo);
		if (product().flow != null) {
			units.fill(product().flow);
			units.select(product().unit, product().property);
		}
		units.listen(item -> {
			product().property = item.property();
			product().unit = item.unit();
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
			if (flow == null) {
				units.clear();
			} else {
				units.fill(flow);
			}
			editor.setDirty();
		});
	}

	private EpdProduct product() {
		var epd = editor.getModel();
		if (epd.product == null) {
			epd.product = new EpdProduct();
			epd.product.amount = 1;
		}
		return epd.product;
	}
}
