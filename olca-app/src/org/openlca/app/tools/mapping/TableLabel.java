package org.openlca.app.tools.mapping;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.mapping.model.FlowMapEntry;
import org.openlca.app.tools.mapping.model.FlowRef;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.core.model.Category;
import org.openlca.io.CategoryPath;

class TableLabel extends LabelProvider
		implements ITableLabelProvider {

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (!(obj instanceof FlowMapEntry))
			return null;
		FlowMapEntry e = (FlowMapEntry) obj;
		if (col == 0)
			return stateIcon(e.syncState);
		return null;
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof FlowMapEntry))
			return null;
		FlowMapEntry e = (FlowMapEntry) obj;

		switch (col) {
		case 0:
			return stateText(e.syncState);
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

	private String stateText(FlowMapEntry.SyncState state) {
		if (state == null)
			return "?";
		switch (state) {
		case APPLIED:
			return "Applied";
		case DUPLICATE:
			return "Duplicate mapping";
		case INVALID_SOURCE:
			return "Invalid source flow";
		case INVALID_TARGET:
			return "Invalid target flow";
		case MATCHED:
			return "Matched";
		case UNFOUND_SOURCE:
			return "Unfound source flow";
		case UNFOUND_TARGET:
			return "Unfound target flow";
		default:
			return "?";
		}
	}

	private Image stateIcon(FlowMapEntry.SyncState state) {
		if (state == null)
			return null;
		switch (state) {
		case APPLIED:
			return Icon.ACCEPT.get();
		case DUPLICATE:
			return Icon.WARNING.get();
		case INVALID_SOURCE:
			return Icon.ERROR.get();
		case INVALID_TARGET:
			return Icon.ERROR.get();
		case MATCHED:
			return Icon.ACCEPT.get();
		case UNFOUND_SOURCE:
			return Icon.WARNING.get();
		case UNFOUND_TARGET:
			return Icon.WARNING.get();
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
		if (ref.flowProperty != null) {
			unit += " (" + ref.flowProperty.name + ")";
		}
		return unit;
	}

}