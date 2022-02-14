package org.openlca.app.editors.epds;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ModelLink;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.FlowPropertyViewer;
import org.openlca.core.model.EpdProduct;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;

import java.util.Objects;

public record EpdProductSection(EpdEditor editor) {

	void render(Composite body, FormToolkit tk) {

		var comp = UI.formSection(body, tk, "Declared product");
		var flowLink = ModelLink.of(Flow.class)
			.renderOn(comp, tk, M.Flow)
			.setModel(product().flow);

		var propCombo = new FlowPropertyViewer(comp);
		if (product().flow != null) {
			propCombo.setInput(
				product().flow.flowPropertyFactors.stream()
					.map(f -> f.flowProperty)
					.filter(Objects::nonNull)
					.toArray(FlowProperty[]::new));
		}
		if (product().property !=  null) {
		}


	}

	private EpdProduct product() {
		var epd = editor.getModel();
		if (epd.product == null) {
			epd.product = new EpdProduct();
		}
		return epd.product;
	}

}
