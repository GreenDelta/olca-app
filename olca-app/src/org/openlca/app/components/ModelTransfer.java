package org.openlca.app.components;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Control;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ActorDescriptor;
import org.openlca.core.model.descriptors.CurrencyDescriptor;
import org.openlca.core.model.descriptors.DQSystemDescriptor;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.EpdDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.LocationDescriptor;
import org.openlca.core.model.descriptors.ParameterDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;
import org.openlca.core.model.descriptors.ProjectDescriptor;
import org.openlca.core.model.descriptors.ResultDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.core.model.descriptors.SocialIndicatorDescriptor;
import org.openlca.core.model.descriptors.SourceDescriptor;
import org.openlca.core.model.descriptors.UnitGroupDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * The transfer type for model descriptors (subclasses of Descriptor). The
 * allowed input is an array of base descriptors. Accordingly, the return type
 * is an array of these descriptors.
 */
public final class ModelTransfer extends ByteArrayTransfer {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private static final String NAME = "model_component_transfer";
	private static final int ID = registerType(NAME);
	private static final ModelTransfer instance = new ModelTransfer();

	private ModelTransfer() {
	}

	public static ModelTransfer getInstance() {
		return instance;
	}

	public static void onDrop(
		Control control, Consumer<List<? extends Descriptor>> fn) {
		if (control == null || fn == null)
			return;
		var target = new DropTarget(
			control, DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_DEFAULT);
		target.setTransfer(instance);
		target.addDropListener(new DropTargetAdapter(){
			@Override
			public void drop(DropTargetEvent e) {
				if (!instance.isSupportedType(e.currentDataType))
					return;
				var ds = getDescriptors(e.data);
				if (!ds.isEmpty()) {
					fn.accept(ds);
				}
			}
		});
	}

	/**
	 * Get the (first) model descriptor from the given transfer data. The given
	 * data should be the data of a respective drop-event (event.data) using
	 * this ModelTransfer class.
	 */
	public static Descriptor getDescriptor(Object data) {
		if (data instanceof Descriptor d)
			return d;
		if (data instanceof Object[] objects) {
			if (objects.length > 0 && (objects[0] instanceof Descriptor d))
				return d;
		}
		return null;
	}

	/**
	 * Get the model descriptors from the given transfer data. The given data
	 * should be the data of a respective drop-event (event.data) using this
	 * ModelTransfer class.
	 */
	public static List<Descriptor> getDescriptors(Object data) {
		if (data instanceof Descriptor d)
			return Collections.singletonList(d);
		if (data instanceof Object[] objects) {
			var descriptors = new ArrayList<Descriptor>();
			for (var object : objects) {
				if (object instanceof Descriptor d)
					descriptors.add(d);
			}
			return descriptors;
		}
		return Collections.emptyList();
	}

	@Override
	protected int[] getTypeIds() {
		return new int[]{ID};
	}

	@Override
	protected String[] getTypeNames() {
		return new String[]{NAME};
	}

	public static Descriptor getDescriptor(DropTargetEvent event) {
		if (event == null
			|| !getInstance().isSupportedType(event.currentDataType))
			return null;
		return getDescriptor(event.data);
	}

	@Override
	protected void javaToNative(Object object, TransferData data) {
		if (!validate(object) || !isSupportedType(data))
			return;
		String json = new Gson().toJson(object);
		super.javaToNative(json.getBytes(StandardCharsets.UTF_8), data);
	}

	@Override
	protected Object nativeToJava(TransferData data) {
		if (!isSupportedType(data))
			return new Object[0];
		Object o = super.nativeToJava(data);
		if (!(o instanceof byte[] bytes))
			return new Object[0];
		try {
			Gson gson = new Gson();
			String json = new String(bytes, StandardCharsets.UTF_8);
			JsonArray array = gson.fromJson(json, JsonArray.class);
			List<Descriptor> list = new ArrayList<>();
			for (JsonElement e : array) {
				Descriptor d = toDescriptor(gson, e);
				if (d != null) {
					list.add(d);
				}
			}
			return list.toArray(new Descriptor[0]);
		} catch (Exception e) {
			log.error("Native to java transfer failed", e);
			return new Object[0];
		}
	}

	private Descriptor toDescriptor(Gson gson, JsonElement e) {
		if (!e.isJsonObject())
			return null;
		JsonElement typeElem = e.getAsJsonObject().get("type");
		if (typeElem == null || !typeElem.isJsonPrimitive())
			return null;
		ModelType type = ModelType.valueOf(typeElem.getAsString());
		return switch (type) {
			case ACTOR -> gson.fromJson(e, ActorDescriptor.class);
			case CATEGORY -> gson.fromJson(e, RootDescriptor.class);
			case CURRENCY -> gson.fromJson(e, CurrencyDescriptor.class);
			case DQ_SYSTEM -> gson.fromJson(e, DQSystemDescriptor.class);
			case EPD -> gson.fromJson(e, EpdDescriptor.class);
			case FLOW -> gson.fromJson(e, FlowDescriptor.class);
			case FLOW_PROPERTY -> gson.fromJson(e, FlowPropertyDescriptor.class);
			case IMPACT_CATEGORY -> gson.fromJson(e, ImpactDescriptor.class);
			case IMPACT_METHOD -> gson.fromJson(e, ImpactMethodDescriptor.class);
			case LOCATION -> gson.fromJson(e, LocationDescriptor.class);
			case PARAMETER -> gson.fromJson(e, ParameterDescriptor.class);
			case PROCESS -> gson.fromJson(e, ProcessDescriptor.class);
			case PRODUCT_SYSTEM -> gson.fromJson(e, ProductSystemDescriptor.class);
			case PROJECT -> gson.fromJson(e, ProjectDescriptor.class);
			case RESULT -> gson.fromJson(e, ResultDescriptor.class);
			case SOCIAL_INDICATOR -> gson.fromJson(e, SocialIndicatorDescriptor.class);
			case SOURCE -> gson.fromJson(e, SourceDescriptor.class);
			case UNIT_GROUP -> gson.fromJson(e, UnitGroupDescriptor.class);
		};
	}

	@Override
	protected boolean validate(Object object) {
		if (!(object instanceof Object[] data))
			return false;
		for (Object d : data) {
			if (!(d instanceof Descriptor))
				return false;
		}
		return true;
	}

}
