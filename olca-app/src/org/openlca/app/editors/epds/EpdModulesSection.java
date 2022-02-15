package org.openlca.app.editors.epds;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.EpdModule;
import org.openlca.core.model.ModelType;
import org.openlca.util.Strings;

import java.util.List;

record EpdModulesSection(EpdEditor editor) {

	void render(Composite body, FormToolkit tk) {

		var section = UI.section(body, tk, "Modules");
		var comp = UI.sectionClient(section, tk);
		var table = Tables.createViewer(comp,
			"Module",
			"Result",
			"LCIA Method",
			"Quantitative reference");
		table.setLabelProvider(new LabelProvider());

		var modules = modules();
		modules.sort((m1, m2) -> Strings.compare(m1.name, m2.name));
		table.setInput(modules);
	}

	private List<EpdModule> modules() {
		return editor.getModel().modules;
	}

	private static class LabelProvider extends BaseLabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col == 0)
				return Images.get(ModelType.RESULT);
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof EpdModule module))
				return null;
			return switch (col) {
				case 0 -> module.name;
				case 1 -> Labels.name(module.result);
				case 2 -> module.result != null
					? Labels.name(module.result.impactMethod)
					: null;
				case 3 -> {
					if (module.result == null
						|| module.result.referenceFlow == null)
						yield null;
					var refFlow = module.result.referenceFlow;
					yield Numbers.format(refFlow.amount, 2)
						+ " " + Labels.name(refFlow.unit)
						+ " " + Labels.name(refFlow.flow);
				}
				default -> null;
			};
		}
	}

}
