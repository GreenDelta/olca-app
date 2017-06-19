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
import org.openlca.app.Preferences;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.UncertaintyLabel;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.CategoryPath;

class ExchangeLabel extends LabelProvider implements ITableLabelProvider,
		ITableColorProvider, ITableFontProvider {

	private final boolean forInputs;
	private final ProcessEditor editor;

	boolean showFormulas = true;

	ExchangeLabel(ProcessEditor editor, boolean forInputs) {
		this.editor = editor;
		this.forInputs = forInputs;
	}

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (!(obj instanceof Exchange))
			return null;
		Exchange e = (Exchange) obj;
		if (col == 0)
			if (e.getFlow() == null)
				return Images.get(ModelType.FLOW);
			else
				return Images.get(e.getFlow());
		if (col == 3)
			return Images.get(ModelType.UNIT);
		if (col == 6 && forInputs && e.getDefaultProviderId() != 0l)
			return Images.get(ModelType.PROCESS);
		if (col == 6 && !forInputs)
			return getAvoidedCheck(e);
		return null;
	}

	private Image getAvoidedCheck(Exchange e) {
		if (e.getFlow() == null)
			return null;
		if (e.getFlow().getFlowType() != FlowType.PRODUCT_FLOW)
			return null;
		Process process = editor.getModel();
		if (Objects.equals(process.getQuantitativeReference(), e))
			return null;
		return e.isAvoidedProduct() ? Icon.CHECK_TRUE.get() : Icon.CHECK_FALSE.get();
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof Exchange))
			return null;
		Exchange e = (Exchange) obj;
		switch (col) {
		case 0:
			return Labels.getDisplayName(e.getFlow());
		case 1:
			if (e.getFlow() == null)
				return null;
			return CategoryPath.getShort(e.getFlow().getCategory());
		case 2:
			return getAmountText(e);
		case 3:
			return Labels.getDisplayName(e.getUnit());
		case 4:
			return getCostValue(e);
		case 5:
			return UncertaintyLabel.get(e.getUncertainty());
		case 6:
			return forInputs ? getDefaultProvider(e) : null;
		case 7:
			return e.getDqEntry();
		case 8:
			return e.description;
		}
		return null;
	}

	private String getDefaultProvider(Exchange e) {
		if (e.getDefaultProviderId() == 0)
			return null;
		EntityCache cache = Cache.getEntityCache();
		ProcessDescriptor p = cache.get(ProcessDescriptor.class,
				e.getDefaultProviderId());
		if (p == null)
			return null;
		return Labels.getDisplayName(p);
	}

	private String getAmountText(Exchange e) {
		if (!showFormulas || e.getAmountFormula() == null) {
			if (Preferences.is(Preferences.FORMAT_INPUT_VALUES)) {
				return Numbers.format(e.getAmountValue());
			} else {
				return Double.toString(e.getAmountValue());
			}
		}
		return e.getAmountFormula();
	}

	private String getCostValue(Exchange e) {
		if (e == null || e.costValue == null)
			return null;
		String unit = e.currency == null ? "" : " " + e.currency.code;
		if (showFormulas && e.costFormula != null)
			return e.costFormula + unit;
		if (Preferences.is(Preferences.FORMAT_INPUT_VALUES))
			return Numbers.format(e.costValue) + unit;
		else
			return Double.toString(e.costValue) + unit;
	}

	@Override
	public Color getBackground(Object obj, int columnIndex) {
		return null;
	}

	@Override
	public Color getForeground(Object obj, int col) {
		if (col != 4)
			return null;
		Exchange e = (Exchange) obj;
		if (e.getFlow() == null)
			return null;
		if (!e.isInput() && e.getFlow().getFlowType() == FlowType.PRODUCT_FLOW)
			return Colors.systemColor(SWT.COLOR_DARK_GREEN);
		else
			return Colors.systemColor(SWT.COLOR_DARK_MAGENTA);
	}

	@Override
	public Font getFont(Object obj, int col) {
		if (!(obj instanceof Exchange))
			return null;
		Exchange e = (Exchange) obj;
		Exchange qRef = editor.getModel().getQuantitativeReference();
		if (Objects.equals(e, qRef))
			return UI.boldFont();
		return null;
	}
}