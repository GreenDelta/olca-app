package org.openlca.app.results;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Labels;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;

public class ResultEditorInput implements IEditorInput {

	private long productSystemId;
	private String resultKey;
	private String setupKey;

	public ResultEditorInput(long productSystemId, String resultKey,
			String setupKey) {
		this.productSystemId = productSystemId;
		this.resultKey = resultKey;
		this.setupKey = setupKey;
	}

	public long getProductSystemId() {
		return productSystemId;
	}

	public String getResultKey() {
		return resultKey;
	}

	public String getSetupKey() {
		return setupKey;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.CHART_ICON.getDescriptor();
	}

	@Override
	public String getName() {
		EntityCache cache = Cache.getEntityCache();
		if (cache == null)
			return "";
		ProductSystemDescriptor d = cache.get(ProductSystemDescriptor.class,
				productSystemId);
		return Messages.Results + ": " + Labels.getDisplayName(d);
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
