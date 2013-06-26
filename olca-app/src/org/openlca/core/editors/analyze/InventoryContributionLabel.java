package org.openlca.core.editors.analyze;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.core.application.Numbers;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.results.AnalysisFlowResult;
import org.openlca.ui.BaseLabelProvider;
import org.openlca.ui.CategoryPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryContributionLabel extends BaseLabelProvider implements
		ITableLabelProvider {

	private Logger log = LoggerFactory.getLogger(getClass());
	private IDatabase database;
	private boolean process;

	public InventoryContributionLabel(IDatabase database, boolean process) {
		super(database);
		this.database = database;
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
			return CategoryPath.getFull(result.getFlow().getCategory());
		case 2:
			return Numbers.format(result.getAggregatedResult());
		case 3:
			return Numbers.format(result.getSingleResult());
		case 4:
			return getReferenceUnitName(database, result.getFlow());
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
