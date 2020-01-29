package org.openlca.app.tools.mapping;

import java.util.function.Function;

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
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.io.CategoryPath;
import org.openlca.io.maps.FlowMapEntry;
import org.openlca.io.maps.FlowRef;
import org.openlca.io.maps.Status;
import org.openlca.util.Strings;

class TableLabel extends LabelProvider
		implements ITableLabelProvider {

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (!(obj instanceof FlowMapEntry))
			return null;
		FlowMapEntry e = (FlowMapEntry) obj;
		if (col == 0)
			return stateIcon(e);
		if (col == 1) {
			if (e.sourceFlow != null)
				return Images.get(e.sourceFlow.flow);
		}
		if (col == 3 && e.targetFlow != null) {
			return Images.get(e.targetFlow.flow);
		}
		if (col == 2 || col == 4)
			return Images.getForCategory(ModelType.FLOW);
		if (col == 5)
			return Icon.FORMULA.get();
		if (col == 6 && e.targetFlow != null
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
			return flow(e.targetFlow);
		case 4:
			return category(e.targetFlow);
		case 5:
			return factor(e);
		case 6:
			return provider(e.targetFlow);
		default:
			return null;
		}

	}

	private String stateText(FlowMapEntry e) {
		if (e == null)
			return "empty mapping";
		if (e.sourceFlow == null)
			return "no source flow";
		if (e.targetFlow == null)
			return "no target flow";

		Status s = status(e);
		if (s == null)
			return "?";
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

	private Image stateIcon(FlowMapEntry e) {
		if (e == null)
			return Icon.ERROR.get();
		if (e.sourceFlow == null)
			return Icon.ERROR.get();
		if (e.targetFlow == null)
			return Icon.ERROR.get();

		Status s = status(e);
		if (s == null)
			return null;

		switch (s.type) {
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

		Status s = e.sourceFlow.status;
		Status t = e.targetFlow.status;

		// when the status is the same => merge it
		if (s.type == t.type) {
			String sm = s.message;
			if (Strings.nullOrEmpty(sm))
				return t;
			String tm = e.targetFlow.status.message;
			if (Strings.nullOrEmpty(tm) || Strings.nullOrEqual(sm, tm))
				return s;
			return new Status(s.type,
					"source flow: " + sm + "; target flow: " + tm);
		}

		// select the status which is worse
		switch (s.type) {
		case Status.OK:
			return new Status(t.type, "target flow: " + t.message);
		case Status.WARNING:
			return t.type == Status.OK
					? new Status(s.type, "source flow: " + s.message)
					: new Status(t.type, "target flow: " + t.message);
		default:
			return new Status(s.type, "source flow: " + s.message);
		}
	}

	private String flow(FlowRef ref) {
		if (ref == null || ref.flow == null)
			return "?";
		if (ref.flow.id != 0L)
			return Labels.name(ref.flow);
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

	private String factor(FlowMapEntry e) {
		if (e == null)
			return "?";
		String f = Numbers.format(e.factor);

		// get the unit and property names
		Function<BaseDescriptor, String> name = d -> {
			if (d == null || d.name == null)
				return "?";
			return d.name;
		};
		String sunit = name.apply(e.sourceFlow != null
				? e.sourceFlow.unit
				: null);
		String sprop = name.apply(e.sourceFlow != null
				? e.sourceFlow.property
				: null);
		String tunit = name.apply(e.targetFlow != null
				? e.targetFlow.unit
				: null);
		String tprop = name.apply(e.targetFlow != null
				? e.targetFlow.property
				: null);

		if (!Strings.nullOrEqual(sprop, tprop)
				&& !("?".equals(sprop) || "?".equals(tprop))) {
			sunit += " (" + sprop + ")";
			tunit += " (" + tprop + ")";
		}
		return f + " " + sunit + "/" + tunit;
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
		if (!Strings.nullOrEmpty(ref.providerLocation)) {
			t += " - " + ref.providerLocation;
		}
		return t;
	}
}