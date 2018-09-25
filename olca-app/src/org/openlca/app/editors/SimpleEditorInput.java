package org.openlca.app.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.util.Strings;

public class SimpleEditorInput implements IEditorInput {

	public final String type;
	public final String id;
	private final String name;

	public SimpleEditorInput(String type, String id, String name) {
		this.type = type;
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
		return getName();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SimpleEditorInput))
			return false;
		SimpleEditorInput input = (SimpleEditorInput) obj;
		return Strings.nullOrEqual(id, input.id);
	}
}
