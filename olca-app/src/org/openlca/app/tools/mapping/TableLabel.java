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
			return stateIcon(status(e));
		if (col == 1) {
			if (e.sourceFlow != null)
				return Images.get(e.sourceFlow.flow);
		}
		if (col == 4 && e.targetFlow != null) {
			return Images.get(e.targetFlow.flow);
		}
		if (col == 2 || col == 5)
			return Images.getForCategory(ModelType.FLOW);
		if (col == 3 || col == 6)
			return Images.get(ModelType.UNIT);
		if (col == 7)
			return Icon.FORMULA.get();
		if (col == 8 && e.targetFlow != null
				&& e.targetFlow.provider != null) {
			return Images.get(e.targetFlow.provider);
		}
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
		case 8:
			return provider(e.targetFlow);
		default:
			return null;
		}

	}

	private String stateText(FlowMapEntry e) {
		Status s = status(e);
		if (s == null)
			return "";
		if (s.message != null)
			return s.message;
		switch (s.type) {
		case Status.OK:
			return "ok";
		case Status.WARNING:
			return "warning";
		case Status.ERROR:
			return "error";
		default:
			return "";
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

	private Status status(FlowMapEntry e) {
		if (e == null)
			return null;
		if (e.sourceFlow == null || e.sourceFlow.status == null)
			return e.targetFlow == null ? null : e.targetFlow.status;
		if (e.targetFlow == null || e.targetFlow.status == null)
			return e.sourceFlow.status;
		if (e.sourceFlow.status.type == e.targetFlow.status.type) {
			return new Status(e.sourceFlow.status.type,
					"source flow: " + e.sourceFlow.status.message
							+ "; target flow: " + e.targetFlow.status.message);
		}
		switch (e.sourceFlow.status.type) {
		case Status.OK:
			return e.targetFlow.status;
		case Status.WARNING:
			return e.targetFlow.status.type == Status.OK
					? e.sourceFlow.status
					: e.targetFlow.status;
		default:
			return e.targetFlow.status;
		}
	}

	private String flow(FlowRef ref) {
		if (ref == null || ref.flow == null)
			return "?";
		if (ref.flow.id != 0L)
			return Labels.getDisplayName(ref.flow);
		String s = ref.flow.name;
		if (s == null) {
			s = ref.flow.refId;
		}
		if (ref.flowLocation != null) {
			s += " - " + ref.flowLocation;
		}
		return s;
	}

	private String category(FlowRef ref) {
		if (ref == null)
			return null;
		if (ref.flowCategory != null)
			return ref.flowCategory;
		if (ref.flow == null || ref.flow.id == 0L)
			return null;
		if (ref.flow.category == null)
			return null;
		Category category = Cache.getEntityCache().get(
				Category.class, ref.flow.category);
		if (category == null)
			return null;
		return CategoryPath.getFull(category);
	}

	private String unit(FlowRef ref) {
		if (ref == null || ref.unit == null)
			return "ref. unit";
		String unit = ref.unit.name;
		if (ref.property != null) {
			unit += " (" + ref.property.name + ")";
		}
		return unit;
	}

	private String provider(FlowRef ref) {
		if (ref == null || ref.provider == null)
			return "";
		String t = ref.provider.name;
		if (t == null) {
			t = ref.provider.refId;
		}
		if (t == null) {
			t = "?";
		}
		if (ref.providerLocation != null) {
			t += " - " + ref.providerLocation;
		}
		return t;
	}
}