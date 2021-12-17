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
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.io.CategoryPath;
import org.openlca.io.maps.FlowMapEntry;
import org.openlca.io.maps.FlowRef;
import org.openlca.io.maps.MappingStatus;
import org.openlca.util.Strings;

class TableLabel extends LabelProvider
	implements ITableLabelProvider {

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (!(obj instanceof FlowMapEntry e))
			return null;

		if (col == 0)
			return stateIcon(e);
		if (col == 1) {
			if (e.sourceFlow() != null)
				return Images.get(e.sourceFlow().flow);
		}
		if (col == 3 && e.targetFlow() != null) {
			return Images.get(e.targetFlow().flow);
		}
		if (col == 2 || col == 4)
			return Images.getForCategory(ModelType.FLOW);
		if (col == 5)
			return Icon.FORMULA.get();
		if (col == 6 && e.targetFlow() != null
			&& e.targetFlow().provider != null) {
			return Images.get(e.targetFlow().provider);
		}
		return null;
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof FlowMapEntry entry))
			return null;
		return switch (col) {
			case 0 -> stateText(entry);
			case 1 -> flow(entry.sourceFlow());
			case 2 -> category(entry.sourceFlow());
			case 3 -> flow(entry.targetFlow());
			case 4 -> category(entry.targetFlow());
			case 5 -> factor(entry);
			case 6 -> provider(entry.targetFlow());
			default -> null;
		};

	}

	private String stateText(FlowMapEntry e) {
		if (e == null)
			return "empty mapping";
		if (e.sourceFlow() == null)
			return "no source flow";
		if (e.targetFlow() == null)
			return "no target flow";

		var s = status(e);
		if (s == null)
			return "?";
		if (s.message() != null)
			return s.message();
		return switch (s.type()) {
			case MappingStatus.OK -> "ok";
			case MappingStatus.WARNING -> "warning";
			case MappingStatus.ERROR -> "error";
			default -> "";
		};
	}

	private Image stateIcon(FlowMapEntry e) {
		if (e == null)
			return Icon.ERROR.get();
		if (e.sourceFlow() == null)
			return Icon.ERROR.get();
		if (e.targetFlow() == null)
			return Icon.ERROR.get();

		var status = status(e);
		if (status == null)
			return null;
		return switch (status.type()) {
			case MappingStatus.OK -> Icon.ACCEPT.get();
			case MappingStatus.WARNING -> Icon.WARNING.get();
			case MappingStatus.ERROR -> Icon.ERROR.get();
			default -> null;
		};
	}

	private MappingStatus status(FlowMapEntry e) {
		if (e == null)
			return null;
		if (e.sourceFlow() == null || e.sourceFlow().status == null)
			return e.targetFlow() == null
				? null
				: e.targetFlow().status;
		if (e.targetFlow() == null || e.targetFlow().status == null)
			return e.sourceFlow().status;

		var s = e.sourceFlow().status;
		var t = e.targetFlow().status;

		// when the status is the same => merge it
		if (s.type() == t.type()) {
			String sm = s.message();
			if (Strings.nullOrEmpty(sm))
				return t;
			String tm = e.targetFlow().status.message();
			if (Strings.nullOrEmpty(tm) || Strings.nullOrEqual(sm, tm))
				return s;
			return new MappingStatus(s.type(),
				"source flow: " + sm + "; target flow: " + tm);
		}

		// select the status which is worse
		return switch (s.type()) {
			case MappingStatus.OK -> new MappingStatus(
				t.type(), "target flow: " + t.message());
			case MappingStatus.WARNING -> t.type() == MappingStatus.OK
				? new MappingStatus(s.type(), "source flow: " + s.message())
				: new MappingStatus(t.type(), "target flow: " + t.message());
			default -> new MappingStatus(s.type(), "source flow: " + s.message());
		};
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
		String f = Numbers.format(e.factor());

		// get the unit and property names
		Function<Descriptor, String> name = d -> {
			if (d == null || d.name == null)
				return "?";
			return d.name;
		};
		String sunit = name.apply(e.sourceFlow() != null
			? e.sourceFlow().unit
			: null);
		String sprop = name.apply(e.sourceFlow() != null
			? e.sourceFlow().property
			: null);
		String tunit = name.apply(e.targetFlow() != null
			? e.targetFlow().unit
			: null);
		String tprop = name.apply(e.targetFlow() != null
			? e.targetFlow().property
			: null);

		if (!Strings.nullOrEqual(sprop, tprop)
			&& !("?".equals(sprop) || "?".equals(tprop))) {
			sunit += " (" + sprop + ")";
			tunit += " (" + tprop + ")";
		}
		return f + " " + tunit + "/" + sunit;
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
