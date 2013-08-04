package org.openlca.app.viewers.combo;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Composite;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;

public class ExchangeViewer extends AbstractComboViewer<Exchange> {

	public static final int INPUTS = 0x01;
	public static final int OUTPUTS = 0x02;
	public static final int PRODUCTS = 0x04;
	public static final int WASTES = 0x08;
	public static final int ELEMENTARIES = 0x10;
	public static final int ALL_DIRECTIONS = INPUTS | OUTPUTS;
	public static final int ALL_TYPES = ELEMENTARIES | PRODUCTS | WASTES;

	private int directions;
	private int types;

	public ExchangeViewer(Composite parent) {
		this(parent, ALL_DIRECTIONS, ALL_TYPES);
	}

	public ExchangeViewer(Composite parent, int directions, int types) {
		super(parent);
		this.directions = directions;
		this.types = types;
		if (this.directions != ALL_DIRECTIONS || this.types != ALL_TYPES)
			getViewer().addFilter(new ExchangeFilter());
	}

	@Override
	public Class<Exchange> getType() {
		return Exchange.class;
	}

	public void setInput(Process process) {
		super.setInput(process.getExchanges().toArray(
				new Exchange[process.getExchanges().size()]));
	}

	private class ExchangeFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if (!(element instanceof Exchange))
				return false;

			Exchange exchange = (Exchange) element;
			boolean inputs = (directions & INPUTS) != 0;
			boolean outputs = (directions & OUTPUTS) != 0;
			boolean products = (types & PRODUCTS) != 0;
			boolean wastes = (types & WASTES) != 0;
			boolean elementaries = (types & ELEMENTARIES) != 0;

			if (!inputs && exchange.isInput())
				return false;
			if (!outputs && !exchange.isInput())
				return false;

			FlowType flowType = exchange.getFlow().getFlowType();
			if (!products && flowType == FlowType.PRODUCT_FLOW)
				return false;
			if (!wastes && flowType == FlowType.WASTE_FLOW)
				return false;
			if (!elementaries && flowType == FlowType.ELEMENTARY_FLOW)
				return false;

			return true;
		}
	}

}
