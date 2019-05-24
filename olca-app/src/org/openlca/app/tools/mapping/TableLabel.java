package org.openlca.app.tools.mapping;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.io.CategoryPath;
import org.openlca.io.maps.FlowMapEntry;
import org.openlca.io.maps.FlowRef;
import org.openlca.io.maps.Status;

class TableLabel extends LabelProvider
		implements ITableLabelProvider {

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (!(obj instanceof FlowMapEntry))
			return null;
		FlowMapEntry e = (FlowMapEntry) obj;
		if (col == 0)
			return stateIcon(e.status);
		if (col == 1) {
			if (e.sourceFlow != null)
				return Images.get(e.sourceFlow.flow);
		}
		if (col == 4) {
			if (e.targetFlow != null)
				return Images.get(e.targetFlow.flow);
		}
		if (col == 2 || col == 5)
			return Images.getForCategory(ModelType.FLOW);
		if (col == 3 || col == 6)
			return Images.get(ModelType.UNIT);
		if (col == 7)
			return Icon.FORMULA.get();
		return null;
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof FlowMapEntry))
			return null;
		FlowMapEntry e = (FlowMapEntry) obj;

		switch (col) {
		case 0:
			return stateText(e);
		case 1:
			return flow(e.sourceFlow);
		case 2:
			return category(e.sourceFlow);
		case 3:
			return unit(e.sourceFlow);
		case 4:
			return flow(e.targetFlow);
		case 5:
			return category(e.targetFlow);
		case 6:
			return unit(e.targetFlow);
		case 7:
			return Numbers.format(e.factor);
		default:
			return null;
		}

	}

	private String stateText(FlowMapEntry e) {
		if (e == null || e.status == null)
			return "?";
		if (e.status.message != null)
			return e.status.message;
		switch (e.status.type) {
		case Status.OK:
			return "ok";
		case Status.WARNING:
			return "warning";
		case Status.ERROR:
			return "error";
		default:
			return "?";
		}
	}

	private Image stateIcon(Status state) {
		if (state == null)
			return null;
		switch (state.type) {
		case Status.OK:
			return Icon.ACCEPT.get();
		case Status.WARNING:
			return Icon.WARNING.get();
		case Status.ERROR:
			return Icon.ERROR.get();
		default:
			return null;
		}
	}

	private String flow(FlowRef ref) {
		if (ref == null || ref.flow == null)
			return "?";
		if (ref.flow.id != 0L)
			return Labels.getDisplayName(ref.flow);
		return ref.flow.name;
	}

	private String category(FlowRef ref) {
		if (ref == null)
			return "?";
		if (ref.categoryPath != null)
			return ref.categoryPath;
		if (ref.flow == null || ref.flow.id == 0L)
			return "?";
		if (ref.flow.category == null)
			return "";
		Category category = Cache.getEntityCache().get(
				Category.class, ref.flow.category);
		if (category == null)
			return "?";
		return CategoryPath.getFull(category);
	}

	private String unit(FlowRef ref) {
		if (ref == null || ref.unit == null)
			return "?";
		String unit = ref.unit.name;
		if (ref.property != null) {
			unit += " (" + ref.property.name + ")";
		}
		return unit;
	}
}