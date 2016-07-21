package org.openlca.app.results;

import java.math.RoundingMode;
import java.util.Collections;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.util.DQUIHelper;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.DQIndicator;
import org.openlca.core.model.DQSystem;

abstract class DQLabelProvider extends ColumnLabelProvider implements ITableLabelProvider, ITableColorProvider {

	protected final DQResult dataQualityResult;
	private final DQSystem dqSystem;
	private final int startCol;

	DQLabelProvider(DQResult result, DQSystem dqSystem, int startCol) {
		this.dataQualityResult = result;
		this.dqSystem = dqSystem;
		this.startCol = startCol;
	}

	protected Image getImage(Object element, int col) {
		return null;
	}

	protected String getText(Object element, int col) {
		return null;
	}

	protected Color getBackgroundColor(Object element, int col) {
		return null;
	}

	protected Color getForegroundColor(Object element, int col) {
		return null;
	}

	protected abstract double[] getQuality(Object obj);

	@Override
	public void update(ViewerCell cell) {
		super.update(cell);
		if (cell == null)
			return;
		Object obj = cell.getElement();
		int col = cell.getColumnIndex();
		cell.setText(getColumnText(obj, col));
		cell.setImage(getColumnImage(obj, col));
		cell.setForeground(getForeground(obj, col));
		cell.setBackground(getBackground(obj, col));
	}

	@Override
	public String getToolTipText(Object element) {
		double[] quality = getQuality(element);
		if (quality == null)
			return null;
		String text = "";
		Collections.sort(dqSystem.indicators);
		for (int i = 0; i < dqSystem.indicators.size(); i++) {
			DQIndicator indicator = dqSystem.indicators.get(i);
			text += indicator.name + ": " + quality[indicator.position - 1];
			if (i != dqSystem.indicators.size() - 1) {
				text += "\n";
			}
		}
		return text;
	}

	@Override
	public final Image getColumnImage(Object element, int col) {
		if (col < startCol)
			return getImage(element, col);
		return null;
	}

	@Override
	public final String getColumnText(Object element, int col) {
		if (col < startCol)
			return getText(element, col);
		int pos = col - startCol;
		double[] quality = getQuality(element);
		if (quality == null)
			return null;
		if (quality[pos] == 0)
			return null;
		double value = quality[pos];
		RoundingMode roundingMode = dataQualityResult.setup.roundingMode;
		int iValue = (int) (roundingMode == RoundingMode.CEILING ? Math.ceil(value) : Math.round(value));
		return Integer.toString(iValue);
	}

	@Override
	public final Color getBackground(Object element, int col) {
		if (col < startCol)
			return getBackgroundColor(element, col);
		int pos = col - startCol;
		double[] quality = getQuality(element);
		if (quality == null)
			return null;
		return DQUIHelper.getColor(quality[pos], dqSystem.getScoreCount(), dataQualityResult.setup.roundingMode);
	}

	@Override
	public final Color getForeground(Object element, int col) {
		if (col < startCol)
			return getForegroundColor(element, col);
		return null;
	}

}
