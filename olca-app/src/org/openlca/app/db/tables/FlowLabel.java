package org.openlca.app.db.tables;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ModelType;
import org.openlca.util.Categories;

class FlowLabel extends LabelProvider implements ITableLabelProvider {

	private final Categories.PathBuilder categories;

	FlowLabel(IDatabase db) {
		this.categories = Categories.pathsOf(db);
	}

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (!(obj instanceof Flow flow))
			return null;
		return switch (col) {
			case 0 -> Images.get(flow.flowType);
			case 2 -> Images.get(flow.category);
			case 3 -> Images.get(ModelType.UNIT);
			case 4 -> Images.get(ModelType.FLOW_PROPERTY);
			default -> null;
		};

	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof Flow flow))
			return null;
		return switch (col) {
			case 0 -> Labels.of(flow.flowType);
			case 1 -> Labels.name(flow);
			case 2 -> flow.category != null
				? categories.pathOf(flow.category.id)
				: null;
			case 3 -> Labels.name(flow.getReferenceUnit());
			case 4 -> Labels.name(flow.referenceFlowProperty);
			case 5 -> flow.casNumber;
			case 6 -> flow.formula;
			case 7 -> flow.refId;
			default -> null;
		};
	}

}
