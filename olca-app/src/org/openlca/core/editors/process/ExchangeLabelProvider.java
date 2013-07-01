package org.openlca.core.editors.process;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Table;
import org.openlca.core.application.Numbers;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.CategoryPath;
import org.openlca.ui.Colors;
import org.openlca.ui.Labels;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ExchangeLabelProvider extends ColumnLabelProvider implements
		ITableLabelProvider, ITableFontProvider {

	private Logger log = LoggerFactory.getLogger(getClass());
	private Process process;
	private IDatabase database;
	private boolean showFormulas = true;
	private Font boldFont;
	private Font italicFont;

	public ExchangeLabelProvider(IDatabase database, Process process) {
		this.process = process;
		this.database = database;
	}

	@Override
	protected void initialize(ColumnViewer viewer, ViewerColumn column) {
		super.initialize(viewer, column);
		Table table = ((TableViewer) viewer).getTable();
		boldFont = UI.boldFont(table);
		italicFont = UI.italicFont(table);
	}

	public void setShowFormulas(boolean showFormulas) {
		this.showFormulas = showFormulas;
	}

	public boolean isShowFormulas() {
		return showFormulas;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
		if (boldFont != null && !boldFont.isDisposed())
			boldFont.dispose();
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (!(element instanceof Exchange))
			return null;
		Exchange exchange = (Exchange) element;
		switch (columnIndex) {
		case 0:
			return getFlowTypeImage(exchange);
		case 6:
			return getAvoidedCheckImage(exchange);
		default:
			return null;
		}
	}

	private Image getFlowTypeImage(Exchange exchange) {
		Flow flow = exchange.getFlow();
		if (flow == null || flow.getFlowType() == null)
			return null;
		switch (flow.getFlowType()) {
		case ELEMENTARY_FLOW:
			return ImageType.FLOW_SUBSTANCE.get();
		case PRODUCT_FLOW:
			return ImageType.FLOW_PRODUCT.get();
		case WASTE_FLOW:
			return ImageType.FLOW_WASTE.get();
		default:
			return null;
		}
	}

	private Image getAvoidedCheckImage(Exchange exchange) {
		Flow flow = exchange.getFlow();
		if (flow == null || flow.getFlowType() == FlowType.ELEMENTARY_FLOW
				|| exchange.isAvoidedProduct() != exchange.isInput())
			return null;
		if (exchange.isAvoidedProduct())
			return ImageType.CHECK_TRUE.get();
		return ImageType.CHECK_FALSE.get();
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof Exchange))
			return null;
		Exchange exchange = (Exchange) element;
		switch (columnIndex) {
		case ExchangeTable.FLOW_COLUMN:
			return exchange.getFlow().getName();
		case ExchangeTable.CATEGORY_COLUM:
			return CategoryPath.getShort(exchange.getFlow().getCategory());
		case ExchangeTable.PROPERTY_COLUMN:
			return exchange.getFlowPropertyFactor().getFlowProperty().getName();
		case ExchangeTable.UNIT_COLUMN:
			return exchange.getUnit().getName();
		case ExchangeTable.AMOUNT_COLUMN:
			return resultingAmount(exchange);
		case ExchangeTable.UNCERTAINTY_COLUMN:
			return Labels.uncertaintyType(exchange.getDistributionType());
		case ExchangeTable.PEDIGREE_COLUMN:
			return pedigreeUncertainty(exchange);
		case ExchangeTable.PROVIDER_COLUMN:
			return providerName(exchange);
		default:
			return null;
		}
	}

	private String resultingAmount(Exchange exchange) {
		if (exchange.getResultingAmount() == null)
			return null;
		if (showFormulas)
			return exchange.getResultingAmount().getFormula();
		return Numbers.format(exchange.getResultingAmount().getValue());
	}

	private String pedigreeUncertainty(Exchange exchange) {
		if (exchange.getPedigreeUncertainty() != null)
			return exchange.getBaseUncertainty() + " | "
					+ exchange.getPedigreeUncertainty();
		return null;
	}

	private String providerName(Exchange exchange) {
		String provId = exchange.getDefaultProviderId();
		if (provId == null)
			return null;
		try {
			ProcessDao dao = new ProcessDao(database.getEntityFactory());
			ProcessDescriptor d = dao.getDescriptor(provId);
			if (d == null)
				return null;
			return d.getDisplayName();
		} catch (Exception e) {
			log.error("Failed to get process descriptor " + provId, e);
			return null;
		}
	}

	@Override
	public String getToolTipText(Object element) {
		if (!(element instanceof Exchange))
			return null;
		Exchange exchange = (Exchange) element;
		return Labels.flowType(exchange.getFlow());
	}

	@Override
	public Font getFont(Object element, int columnIndex) {
		if (!(element instanceof Exchange))
			return null;
		Exchange exchange = (Exchange) element;
		if (exchange.equals(process.getQuantitativeReference()))
			return boldFont;
		if (exchange.isAvoidedProduct())
			return italicFont;
		return null;
	}

	@Override
	public Color getForeground(Object element) {
		if (!(element instanceof Exchange))
			return null;
		Exchange exchange = (Exchange) element;
		if (exchange.isAvoidedProduct())
			return Colors.getDarkGray();
		return null;
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public Color getBackground(Object element) {
		if (!(element instanceof Exchange))
			return null;
		Exchange exchange = (Exchange) element;
		if (exchange.getResultingAmount().getFormula().contains(","))
			return Colors.getErrorColor();
		return null;
	}

}