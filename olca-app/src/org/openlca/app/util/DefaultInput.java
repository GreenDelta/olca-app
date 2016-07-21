package org.openlca.app.util;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.util.Strings;

public class DefaultInput implements IEditorInput {

	public final String id;
	private final String name;
	
	public DefaultInput(String id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
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
	public boolean equals(Object obj) {
		if (!(obj instanceof DefaultInput))
			return false;
		DefaultInput input = (DefaultInput) obj;
		return Strings.nullOrEqual(id, input.id);
	}
}
