package org.openlca.app.editors.parameters;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;

class GlobalParameterInput implements IEditorInput {

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.FORMULA_ICON.getDescriptor();
	}

	@Override
	public String getName() {
		return Messages.GlobalParameters;
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return Messages.GlobalParameters;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof GlobalParameterInput);
	}
}
