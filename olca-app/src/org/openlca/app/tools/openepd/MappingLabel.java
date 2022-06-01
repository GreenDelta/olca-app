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
import org.openlca.core.model.ModelType;
import org.openlca.io.openepd.io.IndicatorMapping;
import org.openlca.io.openepd.io.MethodMapping;

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
		if (col == 1 || col == 3 || col == 4) {
			if (row.epdIndicator() != null && row.unit() == null)
				return Colors.fromHex("#ff5722");
		}
		return null;
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
		var epdInd = row.epdIndicator();
		return switch (col) {
			case 0 -> Labels.name(row.indicator());
			case 1 -> row.indicator() != null
				? row.indicator().referenceUnit
				: null;
			case 2 -> epdInd != null
				? epdInd.code() + " - " + epdInd.description()
				: " - ";
			case 3 -> epdInd != null ? epdInd.unit() : " - ";
			case 4 -> epdInd != null
				? Double.toString(row.factor())
				: " - ";
			default -> {
				int idx = col - 5;
				if (idx < 0 || idx >= mapping.scopes().size())
					yield " - ";
				var scope = mapping.scopes().get(idx);
				var value = row.values().get(scope);
				yield value == null
					? " - "
					: Numbers.format(value * row.factor());
			}
		};
	}



}
