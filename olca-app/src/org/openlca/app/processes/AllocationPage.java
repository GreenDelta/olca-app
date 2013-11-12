package org.openlca.app.processes;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.CategoryPath;
import org.openlca.app.util.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;

class AllocationPage extends FormPage {

	private Process process;
	private ProcessEditor editor;
	private FormToolkit toolkit;

	public AllocationPage(ProcessEditor editor) {
		super(editor, "process.AllocationPage", "Allocation");
		this.process = editor.getModel();
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "Allocation");
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createPhysicalEconomicSection(body);
		createCausalSection(body);
		form.reflow(true);
	}

	private void createPhysicalEconomicSection(Composite body) {
		Section section = UI.section(body, toolkit,
				"Physical & economic allocation");
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		String[] colNames = { Messages.Product, Messages.Physical,
				Messages.Economic };
		TableViewer factorViewer = Tables.createViewer(composite, colNames);
		Tables.bindColumnWidths(factorViewer, 0.3, 0.3, 0.3);
		factorViewer.setLabelProvider(new FactorLabel());
		factorViewer.setInput(getProducts());
	}

	private void createCausalSection(Composite body) {
		Section section = UI.section(body, toolkit, "Causal allocation");
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		String[] colNames = { Messages.Flow, Messages.Direction,
				Messages.Category, Messages.Amount };
		TableViewer causalViewer = Tables.createViewer(composite, colNames);
		Tables.bindColumnWidths(causalViewer, 0.2, 0.1, 0.1, 0.1);
		causalViewer.setLabelProvider(new CausalFactorLabel());
		causalViewer.setInput(getNonProducts());
	}

	private List<Exchange> getProducts() {
		List<Exchange> products = new ArrayList<>();
		for (Exchange exchange : process.getExchanges()) {
			if (exchange.isInput() || exchange.getFlow() == null)
				continue;
			if (exchange.getFlow().getFlowType() == FlowType.PRODUCT_FLOW)
				products.add(exchange);
		}
		return products;
	}

	private List<Exchange> getNonProducts() {
		List<Exchange> nonProducts = new ArrayList<>();
		for (Exchange exchange : process.getExchanges()) {
			if (exchange.getFlow() == null)
				continue;
			if (!exchange.isInput()
					&& exchange.getFlow().getFlowType() == FlowType.PRODUCT_FLOW)
				continue;
			nonProducts.add(exchange);
		}
		return nonProducts;
	}

	private String productText(Exchange exchange) {
		String text = Labels.getDisplayName(exchange.getFlow());
		text += " (" + Numbers.format(exchange.getAmountValue(), 2) + " "
				+ exchange.getUnit().getName() + ")";
		return text;
	}

	private class FactorLabel extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof Exchange))
				return null;
			Exchange exchange = (Exchange) element;
			switch (col) {
			case 0:
				return productText(exchange);
			case 1:
				return Numbers.format(getFactor(exchange,
						AllocationMethod.PHYSICAL));
			case 2:
				return Numbers.format(getFactor(exchange,
						AllocationMethod.ECONOMIC));
			default:
				return null;
			}
		}

		private double getFactor(Exchange exchange, AllocationMethod method) {
			if (exchange == null || method == null)
				return 0;
			AllocationFactor factor = null;
			for (AllocationFactor f : process.getAllocationFactors()) {
				if (f.getAllocationType() == method
						&& f.getProductId() == exchange.getFlow().getId()) {
					factor = f;
					break;
				}
			}
			return factor == null ? 1 : factor.getValue();
		}

		@Override
		public Image getColumnImage(Object element, int col) {
			if (col == 0)
				return ImageType.FLOW_PRODUCT.get();
			return null;
		}
	}

	private class CausalFactorLabel extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			if (col != 0)
				return null;
			if (!(element instanceof Exchange))
				return null;
			Exchange exchange = (Exchange) element;
			if (exchange.getFlow() == null)
				return null;
			return Images.getIcon(exchange.getFlow().getFlowType());
		}

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof Exchange))
				return null;
			Exchange exchange = (Exchange) element;
			if (exchange.getFlow() == null || exchange.getUnit() == null)
				return null;
			switch (col) {
			case 0:
				return Labels.getDisplayName(exchange.getFlow());
			case 1:
				return exchange.isInput() ? "Input" : "Output";
			case 2:
				return CategoryPath.getShort(exchange.getFlow().getCategory());
			case 3:
				return Numbers.format(exchange.getAmountValue()) + " "
						+ exchange.getUnit().getName();

			default:
				break;
			}
			return null;
		}
	}

}
