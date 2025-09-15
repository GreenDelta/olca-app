package org.openlca.app.editors.sd;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class SdResultInput implements IEditorInput {

	private final String modelName;
	private final String key;

	public SdResultInput(String modelName, String key) {
		this.modelName = modelName;
		this.key = key;
	}

	public String modelName() {
		return modelName;
	}

	public String key() {
		return key;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return modelName + " - Simulation Result";
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return getName();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}
}
