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
import org.openlca.app.viewers.combo.FlowViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.IResult;
import org.openlca.core.results.SimpleResult;

/**
 * Two combo boxes showing flows and impact categories. The impact categories
 * are only shown if the are category items available. The user can activate the
 * flow or impact combo and the selection change fires the selection of the
 * current activated combo.
 */
public class ResultTypeSelection {

	private ModelType resultType = ModelType.FLOW;

	private Collection<FlowDescriptor> flows;
	private Collection<ImpactCategoryDescriptor> impacts;
	private Collection<CostResultDescriptor> costs;
	private Object initialSelection;
	private EventHandler eventHandler;

	private FlowViewer flowCombo;
	private ImpactCategoryViewer impactCombo;
	private CostResultViewer costCombo;

	public static Dispatch on(IResult result) {
		ResultTypeSelection selection = new ResultTypeSelection();
		selection.flows = result.getFlows();
		if (result.hasImpactResults())
			selection.impacts = result.getImpacts();
		if (result.hasCostResults() && (result instanceof SimpleResult))
			selection.costs = CostResults.getDescriptors((SimpleResult) result);
		return new Dispatch(selection);
	}

	private ResultTypeSelection() {
	}

	public void selectWithEvent(Object o) {
		if (o == null)
			return;
		if (o instanceof FlowDescriptor)
			selectFlow(o);
		else if (o instanceof ImpactCategoryDescriptor)
			selectImpact(o);
		else if (o instanceof CostResultDescriptor)
			selectCost(o);
	}

	public Object getSelection() {
		switch (resultType) {
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

	private void selectImpact(Object o) {
		resultType = ModelType.IMPACT_CATEGORY;
		if (impactCombo != null) {
			impactCombo.select((ImpactCategoryDescriptor) o);
			impactCombo.setEnabled(true);
		}
		if (flowCombo != null)
			flowCombo.setEnabled(false);
		if (costCombo != null)
			costCombo.setEnabled(false);
	}

	private void selectFlow(Object o) {
		resultType = ModelType.FLOW;
		if (flowCombo != null) {
			flowCombo.select((FlowDescriptor) o);
			flowCombo.setEnabled(true);
		}
		if (impactCombo != null)
			impactCombo.setEnabled(false);
		if (costCombo != null)
			costCombo.setEnabled(false);
	}

	private void selectCost(Object o) {
		resultType = ModelType.CURRENCY;
		if (costCombo != null) {
			costCombo.select((CostResultDescriptor) o);
			costCombo.setEnabled(true);
		}
		if (flowCombo != null)
			flowCombo.setEnabled(false);
		if (impactCombo != null)
			impactCombo.setEnabled(false);
	}

	private void render(Composite parent, FormToolkit toolkit) {
		ModelType initType = getType(initialSelection);
		if (initType != ModelType.UNKNOWN)
			resultType = initType;
		if (flows != null && !flows.isEmpty())
			initFlowCombo(toolkit, parent);
		if (impacts != null && !impacts.isEmpty())
			initImpactCombo(toolkit, parent);
		if (costs != null && !costs.isEmpty())
			initCostCombo(toolkit, parent);
	}

	private void initFlowCombo(FormToolkit toolkit, Composite section) {
		boolean enabled = getType(initialSelection) == ModelType.FLOW;
		Button check = toolkit.createButton(section, M.Flow, SWT.RADIO);
		check.setSelection(enabled);
		flowCombo = new FlowViewer(section);
		flowCombo.setEnabled(enabled);
		FlowDescriptor[] input = flows
				.toArray(new FlowDescriptor[flows.size()]);
		flowCombo.setInput(input);
		flowCombo.selectFirst();
		flowCombo.addSelectionChangedListener((val) -> fireSelection());
		if (enabled)
			flowCombo.select((FlowDescriptor) initialSelection);
		new ResultTypeCheck(flowCombo, check, ModelType.FLOW);
	}

	private void initImpactCombo(FormToolkit toolkit, Composite section) {
		boolean enabled = getType(initialSelection) == ModelType.IMPACT_CATEGORY;
		Button check = toolkit.createButton(section, M.ImpactCategory,
				SWT.RADIO);
		check.setSelection(enabled);
		impactCombo = new ImpactCategoryViewer(section);
		impactCombo.setEnabled(enabled);
		impactCombo.setInput(impacts);
		impactCombo.selectFirst();
		impactCombo.addSelectionChangedListener((val) -> fireSelection());
		if (enabled)
			impactCombo.select((ImpactCategoryDescriptor) initialSelection);
		new ResultTypeCheck(impactCombo, check, ModelType.IMPACT_CATEGORY);
	}

	private void initCostCombo(FormToolkit toolkit, Composite section) {
		boolean enabled = getType(initialSelection) == ModelType.CURRENCY;
		Button check = toolkit.createButton(section, M.CostCategory,
				SWT.RADIO);
		check.setSelection(enabled);
		costCombo = new CostResultViewer(section);
		costCombo.setEnabled(enabled);
		costCombo.setInput(costs);
		costCombo.selectFirst();
		costCombo.addSelectionChangedListener((val) -> fireSelection());
		if (enabled)
			costCombo.select((CostResultDescriptor) initialSelection);
		new ResultTypeCheck(costCombo, check, ModelType.CURRENCY);
	}

	private void fireSelection() {
		if (eventHandler == null || resultType == null)
			return;
		switch (resultType) {
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
		if (o instanceof FlowDescriptor)
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
				resultType = this.type;
				fireSelection();
			} else
				viewer.setEnabled(false);
		}
	}

	/**
	 * The event handler for selection changes.
	 */
	public interface EventHandler {

		void flowSelected(FlowDescriptor flow);

		void impactCategorySelected(ImpactCategoryDescriptor impact);

		void costResultSelected(CostResultDescriptor cost);

	}

	/**
	 * Dispatch class for the initialization of the widgets.
	 */
	public static class Dispatch {

		private ResultTypeSelection selection;

		private Dispatch(ResultTypeSelection selection) {
			this.selection = selection;
		}

		public Dispatch withSelection(Object item) {
			selection.initialSelection = item;
			return this;
		}

		public Dispatch withEventHandler(EventHandler handler) {
			selection.eventHandler = handler;
			return this;
		}

		public ResultTypeSelection create(Composite parent, FormToolkit toolkit) {
			selection.render(parent, toolkit);
			return selection;
		}
	}

}
