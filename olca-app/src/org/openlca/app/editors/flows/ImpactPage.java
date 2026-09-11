package org.openlca.app.editors.flows;

import java.util.Collections;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ModelType;

class ImpactPage extends ModelPage<Flow> {

	ImpactPage(FlowEditor editor) {
		super(editor, "FlowImpactPage", M.ImpactFactors);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.header(this);
		var tk = mform.getToolkit();
		var body = UI.body(form, tk);

		var table = Tables.createViewer(body,
			M.ImpactCategory,
			M.Category,
			M.Location,
			M.ImpactFactor,
			M.Unit);
		table.setLabelProvider(new Label());

		var factors = App.exec("Search characterization factors ...",
			() -> UsedImpactFactor.allOf(getModel(), Database.get()));
		table.setInput(factors != null ? factors : Collections.emptyList());
		Tables.bindColumnWidths(table, 0.2, 0.2, 0.2, 0.2, 0.2);
		var onOpen = Actions.onOpen(() -> {
			UsedImpactFactor f = Viewers.getFirstSelected(table);
			if (f != null) {
				App.open(f.impact());
			}
		});

		Actions.bind(table, onOpen);
		Tables.onDoubleClick(table, _ -> onOpen.run());
		form.reflow(true);
	}

	private static class Label extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof UsedImpactFactor f) || f.impact() == null)
				return null;
			return switch (col) {
				case 0 -> Images.get(f.impact());
				case 1 -> f.impact().category != null
					? Images.getForCategory(ModelType.IMPACT_CATEGORY)
					: null;
				default -> null;
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof UsedImpactFactor f) || f.impact() == null)
				return null;
			return switch (col) {
				case 0 -> Labels.name(f.impact());
				case 1 -> Labels.category(f.impact());
				case 2 -> f.location() != null ? f.location().code : null;
				case 3 -> Numbers.format(f.value());
				case 4 -> {
					var catUnit = f.impact().referenceUnit != null
						? f.impact().referenceUnit
						: "1";
					yield f.unit() != null
						? catUnit + " / " + Labels.name(f.unit())
						: "?";
				}
				default -> null;
			};
		}
	}
}
