/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.core.application.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.resources.ImageType;

/**
 * The basic editor input which contains a model descriptor.
 */
public final class ModelEditorInput implements IEditorInput {

	private IDatabase database;
	private BaseDescriptor descriptor;

	public ModelEditorInput(BaseDescriptor descriptor, IDatabase database) {
		this.descriptor = descriptor;
		this.database = database;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof ModelEditorInput) {
			ModelEditorInput other = (ModelEditorInput) obj;
			return this.descriptor != null && other.descriptor != null
					&& this.descriptor.equals(other.descriptor);
		}
		return false;
	}

	@Override
	public boolean exists() {
		return descriptor != null;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(final Class adapter) {
		return null;
	}

	public IDatabase getDatabase() {
		return database;
	}

	public BaseDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		if (descriptor == null || descriptor.getModelType() == null)
			return null;
		switch (descriptor.getModelType()) {
		case ACTOR:
			return ImageType.ACTOR_ICON.getDescriptor();
		case FLOW:
			return ImageType.FLOW_ICON.getDescriptor();
		case FLOW_PROPERTY:
			return ImageType.FLOW_PROPERTY_ICON.getDescriptor();
		case IMPACT_METHOD:
			return ImageType.LCIA_CATEGORY_ICON.getDescriptor();
		case PROCESS:
			return ImageType.PROCESS_ICON.getDescriptor();
		case PRODUCT_SYSTEM:
			return ImageType.PRODUCT_SYSTEM_ICON.getDescriptor();
		case PROJECT:
			return ImageType.PROJECT_ICON.getDescriptor();
		case SOURCE:
			return ImageType.SOURCE_ICON.getDescriptor();
		case UNIT_GROUP:
			return ImageType.UNIT_GROUP_ICON.getDescriptor();
		default:
			return null;
		}
	}

	@Override
	public String getName() {
		if (descriptor == null)
			return "no content";
		return descriptor.getDisplayName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		if (descriptor == null)
			return "no content";
		return descriptor.getName();
	}

}
