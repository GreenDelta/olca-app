package org.openlca.app.editors.sd;

import java.io.File;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.FileType;

public class SdModelEditorInput implements IEditorInput {

	private final File modelDir;
	private final String name;

	public SdModelEditorInput(File modelDir) {
		this.modelDir = modelDir;
		this.name = modelDir.getName();
	}

	public File getModelDir() {
		return modelDir;
	}

	@Override
	public boolean exists() {
		return modelDir != null && modelDir.exists() && modelDir.isDirectory();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Images.descriptor(FileType.MARKUP);
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
		return "System Dynamics Model: " + name;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SdModelEditorInput other))
			return false;
		return modelDir != null && modelDir.equals(other.modelDir);
	}

	@Override
	public int hashCode() {
		return modelDir != null ? modelDir.hashCode() : 0;
	}
}
