package org.openlca.app.components;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	protected void javaToNative(Object object, TransferData transferData) {
		if (!validate(object) || !isSupportedType(transferData))
			return;
		try (ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(bytesOut)) {
			out.writeObject(object);
			byte[] buffer = bytesOut.toByteArray();
			super.javaToNative(buffer, transferData);
		} catch (IOException e) {
			log.error("Java to native transfer failed", e);
		}
	}

	@Override
	protected Object nativeToJava(TransferData transferData) {
		if (!isSupportedType(transferData))
			return new Object[0];
		Object o = super.nativeToJava(transferData);
		if (!(o instanceof byte[]))
			return new Object[0];
		byte[] bytes = (byte[]) o;
		try (ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
				ObjectInputStream in = new ObjectInputStream(bytesIn)) {
			return in.readObject();
		} catch (Exception e) {
			log.error("Native to java transfer failed", e);
			return new Object[0];
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
