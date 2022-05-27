package org.openlca.app.editors.processes.exchanges;

import java.util.Objects;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

class ExchangeLabel extends LabelProvider implements ITableLabelProvider,
	ITableColorProvider, ITableFontProvider {

	private final ProcessEditor editor;

	boolean showFormulas = true;

	ExchangeLabel(ProcessEditor editor) {
		this.editor = editor;
	}

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (!(obj instanceof Exchange e))
			return null;
		return switch (col) {
			case 0 -> e.flow == null
				? Images.get(ModelType.FLOW)
				: Images.get(e.flow);
			case 3 -> Images.get(ModelType.UNIT);
			case 6 -> getAvoidedCheck(e);
			case 7 -> {
				if (e.defaultProviderId == 0)
					yield null;
				var d = Cache.getEntityCache().get(
					ProcessDescriptor.class, e.defaultProviderId);
				yield d != null ? Images.get(d) : null;
			}
			case 10 -> Images.get(editor.getComments(), CommentPaths.get(e));
			default -> null;
		};
	}

	private Image getAvoidedCheck(Exchange e) {
		if (e.isAvoided)
			return Icon.CHECK_TRUE.get();
		if (e.flow == null)
			return null;
		FlowType type = e.flow.flowType;
		if (type == FlowType.ELEMENTARY_FLOW)
			return null;
		Process process = editor.getModel();
		if (Objects.equals(process.quantitativeReference, e))
			return null;
		if (e.isInput && type == FlowType.WASTE_FLOW)
			return Icon.CHECK_FALSE.get();
		if (!e.isInput && type == FlowType.PRODUCT_FLOW)
			return Icon.CHECK_FALSE.get();
		return null;
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof Exchange e))
			return null;
		return switch (col) {
			case 0 -> Labels.name(e.flow);
			case 1 -> e.flow == null
				? null
				: CategoryPath.getShort(e.flow.category);
			case 2 -> getAmountText(e);
			case 3 -> Labels.name(e.unit);
			case 4 -> getCostValue(e);
			case 5 -> Uncertainty.string(e.uncertainty);
			case 7 -> getDefaultProvider(e);
			case 8 -> {
				// data quality entry
				if (Strings.nullOrEmpty(e.dqEntry))
					yield null;
				Process p = editor.getModel();
				yield p.exchangeDqSystem == null
					? null
					: p.exchangeDqSystem.applyScoreLabels(e.dqEntry);
			}
			case 9 -> e.location == null ? ""
				: e.location.code != null
				? e.location.code
				: Labels.name(e.location);
			case 10 -> e.description;
			default -> null;
		};
	}

	private String getDefaultProvider(Exchange e) {
		if (e.defaultProviderId == 0)
			return null;
		var cache = Cache.getEntityCache();
		var p = cache.get(ProcessDescriptor.class, e.defaultProviderId);
		return p != null
			? Labels.name(p)
			: null;
	}

	private String getAmountText(Exchange e) {
		return !showFormulas || e.formula == null
			? Numbers.format(e.amount)
			: e.formula;
	}

	private String getCostValue(Exchange e) {
		if (e == null || e.costs == null)
			return null;
		var unit = e.currency == null ? "" : " " + e.currency.code;
		return showFormulas && e.costFormula != null
			? e.costFormula + unit
			: Numbers.format(e.costs) + unit;
	}

	@Override
	public Color getBackground(Object obj, int columnIndex) {
		return null;
	}

	@Override
	public Color getForeground(Object obj, int col) {
		// we currently only use this for costs
		if (col != 4)
			return null;
		Exchange e = (Exchange) obj;
		if (e.flow == null || e.costs == null)
			return null;
		FlowType type = e.flow.flowType;
		boolean isRevenue = (e.isInput && type == FlowType.WASTE_FLOW)
			|| (!e.isInput && type == FlowType.PRODUCT_FLOW);
		if ((isRevenue && e.costs >= 0) || (!isRevenue && e.costs < 0))
			return Colors.systemColor(SWT.COLOR_DARK_GREEN);
		else
			return Colors.systemColor(SWT.COLOR_DARK_MAGENTA);
	}

	@Override
	public Font getFont(Object obj, int col) {
		if (!(obj instanceof Exchange e))
			return null;
		var qRef = editor.getModel().quantitativeReference;
		return Objects.equals(e, qRef)
			? UI.boldFont()
			: null;
	}
}
