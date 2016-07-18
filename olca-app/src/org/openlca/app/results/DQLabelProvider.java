package org.openlca.app.results;

import java.math.RoundingMode;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.util.DQUIHelper;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.DQSystem;

abstract class DQLabelProvider extends ColumnLabelProvider implements ITableLabelProvider, ITableColorProvider {

	protected DQResult dataQualityResult;
	private int startCol = 0;

	DQLabelProvider(DQResult result, int startCol) {
		this.startCol = startCol;
		this.dataQualityResult = result;
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

	protected String getToolTipText(Object element, int col) {
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
		return super.getToolTipText(element);
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
		DQSystem system = dataQualityResult.setup.exchangeDqSystem;
		return DQUIHelper.getColor(quality[pos], system.getScoreCount(), dataQualityResult.setup.roundingMode);
	}

	@Override
	public final Color getForeground(Object element, int col) {
		if (col < startCol)
			return getForegroundColor(element, col);
		return null;
	}

}
