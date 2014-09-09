package org.openlca.app.devtools;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class ScriptEditorInput implements IEditorInput {

	private String name;

	public ScriptEditorInput(String name) {
		this.name = name;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return name;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class aClass) {
		return null;
	}
}
