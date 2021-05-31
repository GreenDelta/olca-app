package org.openlca.app.results.comparison;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.util.Strings;

public class ProjectEditorInput implements IEditorInput {
	public final String id;
	private final String name;
	public String projectId;

	public ProjectEditorInput(String id,String projectId, String name) {
		this.id = id;
		this.projectId = projectId;
		this.name = name;
	}

	@Override
	@SuppressWarnings("unchecked")
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
