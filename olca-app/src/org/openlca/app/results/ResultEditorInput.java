package org.openlca.app.results;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Labels;
import org.openlca.core.database.EntityCache;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;
import org.openlca.geo.parameter.ParameterSet;

public class ResultEditorInput implements IEditorInput {

	public final long productSystemId;
	public final String resultKey;
	public final String setupKey;
	public String parameterSetKey;
	public String dqResultKey;

	public ResultEditorInput(long productSystemId, String resultKey, String setupKey) {
		this.productSystemId = productSystemId;
		this.resultKey = resultKey;
		this.setupKey = setupKey;
	}

	public static ResultEditorInput create(CalculationSetup setup, Object result) {
		if (setup == null)
			return null;
		String resultKey = Cache.getAppCache().put(result);
		String setupKey = Cache.getAppCache().put(setup);
		long systemId = 0;
		if (setup.productSystem != null)
			systemId = setup.productSystem.id;
		return new ResultEditorInput(systemId, resultKey, setupKey);
	}

	/** With data quality */
	public ResultEditorInput with(DQResult dqResult) {
		if (dqResult != null)
			dqResultKey = Cache.getAppCache().put(dqResult);
		return this;
	}

	/** With parameters for regionalized calculations. */
	public ResultEditorInput with(ParameterSet parameterSet) {
		if (parameterSet != null)
			parameterSetKey = Cache.getAppCache().put(parameterSet);
		return this;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.CHART.descriptor();
	}

	@Override
	public String getName() {
		EntityCache cache = Cache.getEntityCache();
		if (cache == null)
			return "";
		ProductSystemDescriptor d = cache.get(ProductSystemDescriptor.class,
				productSystemId);
		return M.Results + ": " + Labels.getDisplayName(d);
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
