/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.ui.dnd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The basic transfer type for drag and drop operations of model component
 * descriptors.
 * 
 * @author Michael Srocka
 * 
 */
public final class ModelComponentTransfer extends ByteArrayTransfer {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * Name of the transfer
	 */
	private static final String NAME = "model_component_transfer";

	/**
	 * Id of the transfer
	 */
	private static final int ID = registerType(NAME);

	/** the singleton instance of this transfer type */
	private static ModelComponentTransfer instance;

	/**
	 * The private constructor.
	 */
	private ModelComponentTransfer() {
		// nothing to initialize
	}

	/**
	 * Get the singleton instance of this transfer type.
	 * 
	 * @return the singleton instance of this transfer type
	 */
	public static ModelComponentTransfer getInstance() {
		if (instance == null) {
			instance = new ModelComponentTransfer();
		}
		return instance;
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
	protected void javaToNative(final Object object,
			final TransferData transferData) {
		if (validate(object) && isSupportedType(transferData)) {

			final Object[] objects = (Object[]) object;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (DataOutputStream dos = new DataOutputStream(baos)) {
				// write the model components into a byte array
				for (int i = 0; i < objects.length - 1; i++) {
					IModelComponent component = (IModelComponent) objects[i];
					// key
					String key = component.getId() != null ? component.getId()
							: "";
					dos.writeInt(key.length());
					dos.write(key.getBytes());

					// name
					String name = component.getName() != null ? component
							.getName() : "";
					dos.writeInt(name.length());
					dos.write(name.getBytes());

					// description
					String description = component.getDescription() != null ? component
							.getDescription() : "";
					dos.writeInt(description.length());
					dos.write(description.getBytes());

					// class
					String clazz = component.getClass() != null ? component
							.getClass().getCanonicalName() : "";
					dos.writeInt(clazz.length());
					dos.write(clazz.getBytes());
				}
				dos.writeInt(2);
				dos.write("@@".getBytes());

				IDatabase database = (IDatabase) objects[objects.length - 1];
				String db = database.getUrl();
				dos.writeInt(db.length());
				dos.write(db.getBytes());

				byte[] buffer = baos.toByteArray();
				super.javaToNative(buffer, transferData);

			} catch (IOException e) {
				log.error("Java to native transfer failed", e);
			}
		}

	}

	@Override
	protected Object nativeToJava(TransferData transferData) {

		List<IModelComponent> data = new ArrayList<>();
		String db = null;
		if (isSupportedType(transferData)) {

			Object o = super.nativeToJava(transferData);

			if (o != null && o instanceof byte[]) {

				byte[] buffer = (byte[]) o;

				ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
				try (DataInputStream dis = new DataInputStream(bais)) {
					while (dis.available() > 5) {

						// key
						int keySize = dis.readInt();
						if (keySize == 36) {
							byte[] key = new byte[keySize];
							dis.read(key);

							// name
							int nameSize = dis.readInt();
							byte[] name = new byte[nameSize];
							dis.read(name);

							// description
							int descrSize = dis.readInt();
							byte[] description = new byte[descrSize];
							dis.read(description);

							// class
							int classSize = dis.readInt();
							byte[] clazz = new byte[classSize];
							dis.read(clazz);
							String className = new String(clazz);

							try {

								IModelComponent component = (IModelComponent) ((Class<?>) Class
										.forName(className)).newInstance();
								component.setId(new String(key));
								component.setName(new String(name));
								component
										.setDescription(new String(description));
								data.add(component);
							} catch (ClassNotFoundException e) {
								log.error(
										"Native to java transfer failed (class not found)",
										e);
							} catch (InstantiationException e) {
								log.error(
										"Native to java transfer failed (instantiation failed)",
										e);
							} catch (IllegalAccessException e) {
								log.error(
										"Native to java transfer failed (illegal access)",
										e);
							}
						} else {
							byte[] key = new byte[keySize];
							dis.read(key);
							if (new String(key).equals("@@")) {
								int size = dis.readInt();
								byte[] dbBytes = new byte[size];
								dis.read(dbBytes);
								db = new String(dbBytes);
							}

						}

					}
				} catch (IOException e) {
					log.error("Native to java transfer failed", e);
				}

			}

		}
		Object[] array = new Object[data.size() + 1];
		if (db != null && data.size() > 0) {
			for (int i = 0; i < data.size(); i++) {
				array[i] = data.get(i);
			}
			array[data.size()] = db;
		} else {
			array = new Object[0];
		}
		return array;
	}

	@Override
	protected boolean validate(Object object) {
		boolean valid = object != null && object instanceof Object[];
		if (valid) {
			Object[] data = (Object[]) object;
			for (int i = 0; i < data.length - 1; i++) {
				if (!(data[i] instanceof IModelComponent)) {
					valid = false;
					break;
				}
			}
			if (valid && !(data[data.length - 1] instanceof IDatabase)) {
				valid = false;
			}
		}
		return valid;
	}

}
