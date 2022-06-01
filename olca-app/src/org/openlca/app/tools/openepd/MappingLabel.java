package org.openlca.app.tools.openepd;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.io.openepd.io.IndicatorMapping;
import org.openlca.io.openepd.io.MethodMapping;
import org.openlca.io.openepd.io.Vocab;

class MappingLabel extends LabelProvider
	implements ITableLabelProvider, ITableColorProvider {

	private final MethodMapping mapping;
	private final boolean isForImport;

	private MappingLabel(MethodMapping mapping, boolean isForImport) {
		this.mapping = mapping;
		this.isForImport = isForImport;
	}

	static MappingLabel of(MappingTable table) {
		return new MappingLabel(table.mapping(), table.isForImport());
	}

	@Override
	public Color getForeground(Object obj, int col) {
		if (!(obj instanceof IndicatorMapping row))
			return null;
		boolean isUnitOk = row.epdIndicator() == null
			|| row.indicator() == null
			|| row.unit() != null;
		if (isUnitOk)
			return null;
		return col == 1 || col == 3 || col == 4
			? Colors.fromHex("#ff5722")
			: null;
	}

	@Override
	public Color getBackground(Object obj, int col) {
		return null;
	}

	@Override
	public Image getColumnImage(Object obj, int col) {
		return col == 0
			? Images.get(ModelType.IMPACT_CATEGORY)
			: null;
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof IndicatorMapping row))
			return null;
		return isForImport
			? importTextOf(row, col)
			: exportTextOf(row, col);
	}

	private String importTextOf(IndicatorMapping row, int col) {
		return switch (col) {
			case 0 -> labelOf(row.epdIndicator());
			case 1 -> unitOf(row.epdIndicator());
			case 2 -> labelOf(row.indicator());
			case 3 -> unitOf(row.indicator());
			case 4 -> factorOf(row);
			default -> scopeValueOf(row, col);
		};
	}

	private String exportTextOf(IndicatorMapping row, int col) {
		return switch (col) {
			case 0 -> labelOf(row.indicator());
			case 1 -> unitOf(row.indicator());
			case 2 -> labelOf(row.epdIndicator());
			case 3 -> unitOf(row.epdIndicator());
			case 4 -> factorOf(row);
			default -> scopeValueOf(row, col);
		};
	}

	private String labelOf(Vocab.Indicator epdInd) {
		return epdInd != null
			? epdInd.code() + " - " + epdInd.name()
			: " - ";
	}

	private String unitOf(Vocab.Indicator epdInd) {
		return epdInd != null
			? epdInd.unit()
			: " - ";
	}

	private String labelOf(ImpactCategory ind) {
		return ind != null
			? Labels.name(ind)
			: " - ";
	}

	private String unitOf(ImpactCategory ind) {
		return ind != null
			? ind.referenceUnit
			: " - ";
	}

	private String factorOf(IndicatorMapping row) {
		return row.epdIndicator() != null && row.indicator() != null
			? Double.toString(row.factor())
			: " - ";
	}

	private String scopeValueOf(IndicatorMapping row, int col) {
		int idx = col - 5;
		if (idx < 0 || idx > mapping.scopes().size())
			return " - ";
		var scope = mapping.scopes().get(idx);
		var value = row.values().get(scope);
		return value != null
			? Numbers.format(value * row.factor())
			: " - ";
	}

}
