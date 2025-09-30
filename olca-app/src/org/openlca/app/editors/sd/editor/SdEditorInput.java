package org.openlca.app.editors.sd.editor;

import java.io.File;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.FileType;

record SdEditorInput(File dir, String key) implements IEditorInput {

	@Override
	public boolean exists() {
		return dir != null && dir.exists() && dir.isDirectory();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Images.descriptor(FileType.MARKUP);
	}

	@Override
	public String getName() {
		return dir.getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return "System dynamics model: " + getName();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof SdEditorInput other))
			return false;
		return dir != null && dir.equals(other.dir);
	}

	@Override
	public int hashCode() {
		return dir != null ? dir.hashCode() : 0;
	}
}
