package org.openlca.app.editors.projects.results;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.editors.projects.ProjectResultData;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProjectVariant;

class ProjectVariantSection extends LabelProvider implements TableSection {

	private final ProjectResultData data;

	private ProjectVariantSection(ProjectResultData data) {
		this.data = data;
	}

	static ProjectVariantSection of(ProjectResultData data) {
		return new ProjectVariantSection(data);
	}

	@Override
	public void renderOn(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, M.Variants);
		var comp = UI.sectionClient(section, tk, 1);
		var table = Tables.createViewer(comp,
			M.Variant,
			M.ProductSystem,
			M.AllocationMethod,
			M.Amount,
			M.Unit);

		table.setLabelProvider(this);
		Viewers.sortByLabels(table, this, 0, 1, 2, 4);
		Viewers.sortByDouble(table, this, 3);
		Tables.bindColumnWidths(table, 0.2, 0.2, 0.2, 0.2, 0.2);
		Actions.bind(section, TableClipboard.onCopyAll(table));
		Actions.bind(table, TableClipboard.onCopySelected(table));
		table.setInput(data.variants());
	}

	@Override
	public Image getColumnImage(Object obj, int col) {
		return switch (col) {
			case 0 -> Images.get(ModelType.PROJECT);
			case 1 -> Images.get(ModelType.PRODUCT_SYSTEM);
			case 4 -> Images.get(ModelType.UNIT);
			default -> null;
		};
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof ProjectVariant))
			return null;
		var variant = (ProjectVariant) obj;
		return switch (col) {
			case 0 -> variant.name;
			case 1 -> Labels.name(variant.productSystem);
			case 2 -> Labels.of(variant.allocationMethod);
			case 3 -> Numbers.format(variant.amount);
			case 4 -> {
				var unit = Labels.name(variant.unit);
				var prop = variant.flowPropertyFactor != null
					? Labels.name(variant.flowPropertyFactor.flowProperty)
					: null;
				yield prop != null
					? unit + " (" + prop + ")"
					: unit;
			}
			default -> null;
		};
	}
}
