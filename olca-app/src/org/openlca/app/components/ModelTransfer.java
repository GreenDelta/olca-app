package org.openlca.app.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ActorDescriptor;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.CurrencyDescriptor;
import org.openlca.core.model.descriptors.DQSystemDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.LocationDescriptor;
import org.openlca.core.model.descriptors.NwSetDescriptor;
import org.openlca.core.model.descriptors.ParameterDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;
import org.openlca.core.model.descriptors.ProjectDescriptor;
import org.openlca.core.model.descriptors.SocialIndicatorDescriptor;
import org.openlca.core.model.descriptors.SourceDescriptor;
import org.openlca.core.model.descriptors.UnitDescriptor;
import org.openlca.core.model.descriptors.UnitGroupDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * The transfer type for model descriptors (subclasses of BaseDescriptor). The
 * allowed input is an array of base descriptors. Accordingly, the return type
 * is an array of these descriptors.
 */
public final class ModelTransfer extends ByteArrayTransfer {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private static final String NAME = "model_component_transfer";
	private static final int ID = registerType(NAME);
	private static ModelTransfer instance;

	private ModelTransfer() {
	}

	public static ModelTransfer getInstance() {
		if (instance == null) {
			instance = new ModelTransfer();
		}
		return instance;
	}

	/**
	 * Get the (first) model descriptor from the given transfer data. The given
	 * data should be the data of a respective drop-event (event.data) using
	 * this ModelTransfer class.
	 */
	public static BaseDescriptor getDescriptor(Object data) {
		if (data instanceof BaseDescriptor)
			return (BaseDescriptor) data;
		if (data instanceof Object[]) {
			Object[] objects = (Object[]) data;
			if (objects.length > 0 && (objects[0] instanceof BaseDescriptor))
				return (BaseDescriptor) objects[0];
		}
		return null;
	}

	/**
	 * Get the model descriptors from the given transfer data. The given data
	 * should be the data of a respective drop-event (event.data) using this
	 * ModelTransfer class.
	 */
	public static List<BaseDescriptor> getBaseDescriptors(Object data) {
		if (data instanceof BaseDescriptor)
			return Arrays.asList((BaseDescriptor) data);
		if (data instanceof Object[]) {
			Object[] objects = (Object[]) data;
			ArrayList<BaseDescriptor> descriptors = new ArrayList<>();
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof BaseDescriptor)
					descriptors.add((BaseDescriptor) objects[i]);
			}
			return descriptors;
		}
		return Collections.emptyList();
	}

	@Override
	protected int[] getTypeIds() {
		return new int[] { ID };
	}

	@Override
	protected String[] getTypeNames() {
		return new String[] { NAME };
	}

	@Override
	protected void javaToNative(Object object, TransferData data) {
		if (!validate(object) || !isSupportedType(data))
			return;
		try {
			String json = new Gson().toJson(object);
			super.javaToNative(json.getBytes("utf-8"), data);
		} catch (IOException e) {
			log.error("Java to native transfer failed", e);
		}
	}

	@Override
	protected Object nativeToJava(TransferData data) {
		if (!isSupportedType(data))
			return new Object[0];
		Object o = super.nativeToJava(data);
		if (!(o instanceof byte[]))
			return new Object[0];
		byte[] bytes = (byte[]) o;
		try {
			Gson gson = new Gson();
			String json = new String(bytes, "utf-8");
			JsonArray array = gson.fromJson(json, JsonArray.class);
			List<BaseDescriptor> list = new ArrayList<>();
			for (JsonElement e : array) {
				BaseDescriptor d = toDescriptor(gson, e);
				if (d != null) {
					list.add(d);
				}
			}
			BaseDescriptor[] descriptors = list.toArray(
					new BaseDescriptor[list.size()]);
			return descriptors;
		} catch (Exception e) {
			log.error("Native to java transfer failed", e);
			return new Object[0];
		}
	}

	private BaseDescriptor toDescriptor(Gson gson, JsonElement e) {
		if (!e.isJsonObject())
			return null;
		JsonElement typeElem = e.getAsJsonObject().get("type");
		if (typeElem == null || !typeElem.isJsonPrimitive())
			return null;
		ModelType type = ModelType.valueOf(typeElem.getAsString());
		if (type == null)
			return null;
		switch (type) {
		case ACTOR:
			return gson.fromJson(e, ActorDescriptor.class);
		case CATEGORY:
			return gson.fromJson(e, CategorizedDescriptor.class);
		case CURRENCY:
			return gson.fromJson(e, CurrencyDescriptor.class);
		case DQ_SYSTEM:
			return gson.fromJson(e, DQSystemDescriptor.class);
		case FLOW:
			return gson.fromJson(e, FlowDescriptor.class);
		case FLOW_PROPERTY:
			return gson.fromJson(e, FlowPropertyDescriptor.class);
		case IMPACT_CATEGORY:
			return gson.fromJson(e, ImpactCategoryDescriptor.class);
		case IMPACT_METHOD:
			return gson.fromJson(e, ImpactMethodDescriptor.class);
		case LOCATION:
			return gson.fromJson(e, LocationDescriptor.class);
		case NW_SET:
			return gson.fromJson(e, NwSetDescriptor.class);
		case PARAMETER:
			return gson.fromJson(e, ParameterDescriptor.class);
		case PROCESS:
			return gson.fromJson(e, ProcessDescriptor.class);
		case PRODUCT_SYSTEM:
			return gson.fromJson(e, ProductSystemDescriptor.class);
		case PROJECT:
			return gson.fromJson(e, ProjectDescriptor.class);
		case SOCIAL_INDICATOR:
			return gson.fromJson(e, SocialIndicatorDescriptor.class);
		case SOURCE:
			return gson.fromJson(e, SourceDescriptor.class);
		case UNIT:
			return gson.fromJson(e, UnitDescriptor.class);
		case UNIT_GROUP:
			return gson.fromJson(e, UnitGroupDescriptor.class);
		default:
			if (type.isCategorized())
				return gson.fromJson(e, CategorizedDescriptor.class);
			else
				return gson.fromJson(e, BaseDescriptor.class);
		}
	}

	@Override
	protected boolean validate(Object object) {
		if (!(object instanceof Object[]))
			return false;
		Object[] data = (Object[]) object;
		for (Object d : data) {
			if (!(d instanceof BaseDescriptor))
				return false;
		}
		return true;
	}

}
