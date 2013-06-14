/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.io.ui;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.modelprovider.IModelComponent;

/**
 * Wraps a process together with the database the process is saved in
 * 
 * @author Sebastian Greve
 * 
 */
public class ObjectWrapper {

	/**
	 * The model component
	 */
	private final IModelComponent modelComponent;

	/**
	 * The database
	 */
	private final IDatabase database;

	/**
	 * Creates a new {@link ObjectWrapper}
	 * 
	 * @param modelComponent
	 *            The model component
	 * @param database
	 *            The database the process is saved in
	 */
	public ObjectWrapper(final IModelComponent modelComponent,
			final IDatabase database) {
		this.modelComponent = modelComponent;
		this.database = database;
	}

	@Override
	public boolean equals(final Object obj) {
		boolean equals = false;
		if (obj instanceof ObjectWrapper) {
			final ObjectWrapper wrapper = (ObjectWrapper) obj;
			if (wrapper.getModelComponent().getId()
					.equals(modelComponent.getId())) {
				if (wrapper.getDatabase().equals(getDatabase())) {
					equals = true;
				}
			}
		}
		return equals;
	}

	/**
	 * Getter of the model component
	 * 
	 * @return The model component
	 */
	public IModelComponent getModelComponent() {
		return modelComponent;
	}

	/**
	 * Getter of the database
	 * 
	 * @return The the database the process is saved in
	 */
	public IDatabase getDatabase() {
		return database;
	}

}
