package org.openlca.app.editors.sd.editor;

import java.io.File;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.FileType;
import org.openlca.app.util.SystemDynamics;

record SdEditorInput(File dir) implements IEditorInput {

	public static SdEditorInput of(File dir) {
		return dir != null
			? new SdEditorInput(dir)
			: null;
	}

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
		return SystemDynamics.modelNameOf(dir);
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
