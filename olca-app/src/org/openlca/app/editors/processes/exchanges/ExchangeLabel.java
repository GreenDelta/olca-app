package org.openlca.app.editors.processes.exchanges;

import java.util.Objects;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UncertaintyLabel;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.CategoryPath;

class ExchangeLabel extends LabelProvider implements ITableLabelProvider {

	private final boolean forInputs;
	private final ProcessEditor editor;

	boolean showFormulas = true;

	ExchangeLabel(ProcessEditor editor, boolean forInputs) {
		this.editor = editor;
		this.forInputs = forInputs;
	}

	@Override
	public Image getColumnImage(Object element, int col) {
		if (!(element instanceof Exchange))
			return null;
		Exchange exchange = (Exchange) element;
		if (col == 0)
			return getFlowTypeIcon(exchange);
		if (!forInputs && col == 6)
			return getAvoidedCheck(exchange);
		return null;
	}

	private Image getAvoidedCheck(Exchange exchange) {
		if (exchange.getFlow() == null)
			return null;
		if (exchange.getFlow().getFlowType() != FlowType.PRODUCT_FLOW)
			return null;
		Process process = editor.getModel();
		if (Objects.equals(process.getQuantitativeReference(), exchange))
			return null;
		else
			return exchange.isAvoidedProduct() ? ImageType.CHECK_TRUE.get()
					: ImageType.CHECK_FALSE.get();
	}

	private Image getFlowTypeIcon(Exchange exchange) {
		if (exchange == null)
			return null;
		Flow flow = exchange.getFlow();
		if (flow == null || flow.getFlowType() == null)
			return null;
		switch (flow.getFlowType()) {
		case ELEMENTARY_FLOW:
			return ImageType.FLOW_ELEMENTARY.get();
		case PRODUCT_FLOW:
			return ImageType.FLOW_PRODUCT.get();
		case WASTE_FLOW:
			return ImageType.FLOW_WASTE.get();
		default:
			return null;
		}
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof Exchange))
			return null;
		Exchange exchange = (Exchange) obj;
		switch (col) {
		case 0:
			return Labels.getDisplayName(exchange.getFlow());
		case 1:
			return CategoryPath.getShort(exchange.getFlow().getCategory());
		case 2:
			return getAmountText(exchange);
		case 3:
			return Labels.getDisplayName(exchange.getUnit());
		case 4:
			return getCostValue(exchange);
		case 5:
			return UncertaintyLabel.get(exchange.getUncertainty());
		case 6:
			if (forInputs)
				return getDefaultProvider(exchange);
			else
				return null;
		case 7:
			return exchange.getPedigreeUncertainty();
		case 8:
			return exchange.description;
		}
		return null;
	}

	private String getDefaultProvider(Exchange exchange) {
		if (exchange.getDefaultProviderId() == 0)
			return null;
		EntityCache cache = Cache.getEntityCache();
		ProcessDescriptor descriptor = cache.get(ProcessDescriptor.class,
				exchange.getDefaultProviderId());
		if (descriptor == null)
			return null;
		return Labels.getDisplayName(descriptor);
	}

	private String getAmountText(Exchange exchange) {
		if (!showFormulas || exchange.getAmountFormula() == null)
			return Double.toString(exchange.getAmountValue());
		else
			return exchange.getAmountFormula();
	}

	private String getCostValue(Exchange exchange) {
		if (exchange == null || exchange.costValue == null)
			return null;
		String s;
		if (showFormulas && exchange.costFormula != null) {
			s = exchange.costFormula;
		} else {
			s = exchange.costValue.toString();
		}
		if (exchange.currency == null)
			return s;
		else
			return s + " " + exchange.currency.code;
	}

	CellLabelProvider asColumnLabel() {
		return new ColumnLabel();
	}

	private class ColumnLabel extends ColumnLabelProvider {

		@Override
		public void update(ViewerCell cell) {
			super.update(cell);
			if (cell == null)
				return;
			Object obj = cell.getElement();
			int col = cell.getColumnIndex();
			cell.setText(getColumnText(obj, col));
			cell.setImage(getColumnImage(obj, col));
			if (col == 4)
				setCostColor(cell);
		}

		private void setCostColor(ViewerCell cell) {
			if (cell == null)
				return;
			Object obj = cell.getElement();
			if (!(obj instanceof Exchange))
				return;
			Exchange e = (Exchange) obj;
			if (e.getFlow() == null)
				return;
			if (!e.isInput()
					&& e.getFlow().getFlowType() == FlowType.PRODUCT_FLOW)
				cell.setForeground(Colors.getSystemColor(SWT.COLOR_DARK_GREEN));
			else
				cell.setForeground(Colors
						.getSystemColor(SWT.COLOR_DARK_MAGENTA));
		}
	}

}