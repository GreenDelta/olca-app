package org.openlca.app.editors;

import java.util.Objects;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Strings;

/**
 * The basic editor input which contains a model descriptor.
 */
public final class ModelEditorInput implements IEditorInput {

	private final Descriptor descriptor;

	public ModelEditorInput(Descriptor descriptor) {
		this.descriptor = descriptor;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof ModelEditorInput) {
			var other = (ModelEditorInput) obj;
			return Objects.equals(this.descriptor, other.descriptor);
		}
		return false;
	}

	@Override
	public boolean exists() {
		return descriptor != null;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object getAdapter(final Class adapter) {
		return null;
	}

	public Descriptor getDescriptor() {
		return descriptor;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		if (descriptor == null)
			return null;
		return Images.descriptor(descriptor);
	}

	@Override
	public String getName() {
		if (descriptor == null)
			return "no content";
		return Strings.cut(Labels.name(descriptor), 75);
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		if (descriptor == null)
			return "no content";
		return Labels.name(descriptor);
	}

}
