package org.openlca.app.components;

import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.CostResults;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.app.viewers.combo.CostResultViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.IResult;
import org.openlca.core.results.SimpleResult;

/**
 * Multiple combo boxes that allow to switch between different result types
 * (flows, LCIA categories, costs). The respective items are only shown if the
 * corresponding result is available.
 */
public class ResultTypeCombo {

	private ModelType selectedType = ModelType.FLOW;

	private Collection<IndexFlow> flows;
	private Collection<ImpactCategoryDescriptor> impacts;
	private Collection<CostResultDescriptor> costs;
	private Object initialSelection;
	private EventHandler eventHandler;

	private ResultFlowCombo flowCombo;
	private ImpactCategoryViewer impactCombo;
	private CostResultViewer costCombo;

	public static Builder on(IResult r) {
		ResultTypeCombo c = new ResultTypeCombo();
		c.flows = r.getFlows();
		if (r.hasImpactResults()) {
			c.impacts = r.getImpacts();
		}
		if (r.hasCostResults() && (r instanceof SimpleResult)) {
			c.costs = CostResults.getDescriptors((SimpleResult) r);
		}
		return new Builder(c);
	}

	private ResultTypeCombo() {
	}

	public void selectWithEvent(Object o) {
		if (o == null)
			return;
		if (o instanceof IndexFlow) {
			selectFlow((IndexFlow) o);
		} else if (o instanceof ImpactCategoryDescriptor) {
			selectImpact((ImpactCategoryDescriptor) o);
		} else if (o instanceof CostResultDescriptor) {
			selectCost((CostResultDescriptor) o);
		}
	}

	public void initWithEvent() {

	}

	public Object getSelection() {
		switch (selectedType) {
		case FLOW:
			return flowCombo.getSelected();
		case IMPACT_CATEGORY:
			return impactCombo.getSelected();
		case CURRENCY:
			return costCombo.getSelected();
		default:
			return null;
		}
	}

	private void selectImpact(ImpactCategoryDescriptor impact) {
		selectedType = ModelType.IMPACT_CATEGORY;
		if (impactCombo != null) {
			impactCombo.select(impact);
			impactCombo.setEnabled(true);
		}
		if (flowCombo != null) {
			flowCombo.setEnabled(false);
		}
		if (costCombo != null) {
			costCombo.setEnabled(false);
		}
	}

	private void selectFlow(IndexFlow flow) {
		selectedType = ModelType.FLOW;
		if (flowCombo != null) {
			flowCombo.select(flow);
			flowCombo.setEnabled(true);
		}
		if (impactCombo != null) {
			impactCombo.setEnabled(false);
		}
		if (costCombo != null) {
			costCombo.setEnabled(false);
		}
	}

	private void selectCost(CostResultDescriptor costs) {
		selectedType = ModelType.CURRENCY;
		if (costCombo != null) {
			costCombo.select(costs);
			costCombo.setEnabled(true);
		}
		if (flowCombo != null) {
			flowCombo.setEnabled(false);
		}
		if (impactCombo != null) {
			impactCombo.setEnabled(false);
		}
	}

	private void render(Composite comp, FormToolkit tk) {
		ModelType initType = getType(initialSelection);
		if (initType != ModelType.UNKNOWN)
			selectedType = initType;
		if (flows != null && !flows.isEmpty())
			initFlowCombo(tk, comp);
		if (impacts != null && !impacts.isEmpty())
			initImpactCombo(tk, comp);
		if (costs != null && !costs.isEmpty())
			initCostCombo(tk, comp);
	}

	private void initFlowCombo(FormToolkit tk, Composite comp) {
		boolean enabled = getType(initialSelection) == ModelType.FLOW;
		Button check = tk.createButton(comp, M.Flow, SWT.RADIO);
		check.setSelection(enabled);
		flowCombo = new ResultFlowCombo(comp);
		flowCombo.setEnabled(enabled);
		IndexFlow[] input = flows.toArray(new IndexFlow[0]);
		flowCombo.setInput(input);
		flowCombo.selectFirst();
		flowCombo.addSelectionChangedListener(_e -> fireSelection());
		if (enabled) {
			flowCombo.select((IndexFlow) initialSelection);
		}
		new ResultTypeCheck(flowCombo, check, ModelType.FLOW);
	}

	private void initImpactCombo(FormToolkit tk, Composite comp) {
		boolean enabled = getType(initialSelection) == ModelType.IMPACT_CATEGORY;
		Button check = tk.createButton(comp, M.ImpactCategory, SWT.RADIO);
		check.setSelection(enabled);
		impactCombo = new ImpactCategoryViewer(comp);
		impactCombo.setEnabled(enabled);
		impactCombo.setInput(impacts);
		impactCombo.selectFirst();
		impactCombo.addSelectionChangedListener((val) -> fireSelection());
		if (enabled) {
			impactCombo.select((ImpactCategoryDescriptor) initialSelection);
		}
		new ResultTypeCheck(impactCombo, check, ModelType.IMPACT_CATEGORY);
	}

	private void initCostCombo(FormToolkit tk, Composite comp) {
		boolean enabled = getType(initialSelection) == ModelType.CURRENCY;
		Button check = tk.createButton(comp, M.CostCategory, SWT.RADIO);
		check.setSelection(enabled);
		costCombo = new CostResultViewer(comp);
		costCombo.setEnabled(enabled);
		costCombo.setInput(costs);
		costCombo.selectFirst();
		costCombo.addSelectionChangedListener((val) -> fireSelection());
		if (enabled) {
			costCombo.select((CostResultDescriptor) initialSelection);
		}
		new ResultTypeCheck(costCombo, check, ModelType.CURRENCY);
	}

	private void fireSelection() {
		if (eventHandler == null || selectedType == null)
			return;
		switch (selectedType) {
		case FLOW:
			eventHandler.flowSelected(flowCombo.getSelected());
			break;
		case IMPACT_CATEGORY:
			eventHandler.impactCategorySelected(impactCombo.getSelected());
			break;
		case CURRENCY:
			eventHandler.costResultSelected(costCombo.getSelected());
			break;
		default:
			break;
		}
	}

	private ModelType getType(Object o) {
		if (o instanceof IndexFlow)
			return ModelType.FLOW;
		else if (o instanceof ImpactCategoryDescriptor)
			return ModelType.IMPACT_CATEGORY;
		else if (o instanceof CostResultDescriptor)
			return ModelType.CURRENCY;
		else
			return ModelType.UNKNOWN;
	}

	private class ResultTypeCheck implements SelectionListener {

		private AbstractComboViewer<?> viewer;
		private Button check;
		private ModelType type;

		public ResultTypeCheck(AbstractComboViewer<?> viewer, Button check,
				ModelType type) {
			this.viewer = viewer;
			this.check = check;
			this.type = type;
			check.addSelectionListener(this);
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			if (check.getSelection()) {
				viewer.setEnabled(true);
				selectedType = this.type;
				fireSelection();
			} else {
				viewer.setEnabled(false);
			}
		}
	}

	public interface EventHandler {

		void flowSelected(IndexFlow flow);

		void impactCategorySelected(ImpactCategoryDescriptor impact);

		void costResultSelected(CostResultDescriptor cost);

	}

	/**
	 * Builder class for the initialization of the widgets.
	 */
	public static class Builder {

		private ResultTypeCombo selection;

		private Builder(ResultTypeCombo selection) {
			this.selection = selection;
		}

		public Builder withSelection(Object item) {
			selection.initialSelection = item;
			return this;
		}

		public Builder withEventHandler(EventHandler handler) {
			selection.eventHandler = handler;
			return this;
		}

		public ResultTypeCombo create(Composite comp, FormToolkit tk) {
			selection.render(comp, tk);
			return selection;
		}
	}

}
