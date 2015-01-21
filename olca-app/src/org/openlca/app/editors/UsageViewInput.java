package org.openlca.app.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Labels;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.descriptors.BaseDescriptor;

/**
 * Input for the usage view. Contains the descriptor of the model which usages
 * should be shown.
 */
class UsageViewInput implements IEditorInput {

	private BaseDescriptor descriptor;
	private IDatabase database;

	public UsageViewInput(BaseDescriptor descriptor, IDatabase database) {
		this.descriptor = descriptor;
		this.database = database;
	}

	public BaseDescriptor getDescriptor() {
		return descriptor;
	}

	public IDatabase getDatabase() {
		return database;
	}

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
		return ImageType.LINK_16_BLUE.getDescriptor();
	}

	@Override
	public String getName() {
		return descriptor != null ? Labels.getDisplayName(descriptor) : "";
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return getName();
	}

}
