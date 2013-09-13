package org.openlca.app.analysis;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.util.CategoryPath;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.core.database.EntityCache;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.model.Flow;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.results.AnalysisFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryContributionLabel extends BaseLabelProvider implements
		ITableLabelProvider {

	private EntityCache cache;
	private boolean process;

	public InventoryContributionLabel(EntityCache cache, boolean process) {
		this.cache = cache;
		this.process = process;
	}

	@Override
	public void addListener(final ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public Image getColumnImage(final Object element, final int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof AnalysisFlowResult))
			return null;
		AnalysisFlowResult result = (AnalysisFlowResult) element;
		columnIndex = columnIndex > 0 && process ? columnIndex + 1
				: columnIndex;
		switch (columnIndex) {
		case 0:
			return process ? result.getProcess().getName() : result.getFlow()
					.getName();
		case 1:
			Long catId = result.getFlow().getCategory();
			if (catId == null)
				return null;
			return CategoryPath.getFull(cache.get(Category.class, catId));
		case 2:
			return Numbers.format(result.getTotalResult());
		case 3:
			return Numbers.format(result.getSingleResult());
		case 4:
			return Labels.getRefUnit(result.getFlow(), cache);
		default:
			return null;
		}
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	static String getReferenceUnitName(IDatabase database, Flow flow) {
		try {
			UnitGroup unitGroup = flow.getReferenceFlowProperty()
					.getUnitGroup();
			return unitGroup.getReferenceUnit().getName();
		} catch (final Exception e) {
			Logger log = LoggerFactory
					.getLogger(InventoryContributionLabel.class);
			log.error("Failed to get reference unit name", e);
			return null;
		}
	}
}
