package org.openlca.app.components;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.util.Controls;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.app.viewers.combo.CostResultViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.matrix.index.IndexFlow;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.IResult;
import org.openlca.core.results.SimpleResult;

/**
 * Multiple combo boxes that allow to switch between different result types
 * (flows, LCIA categories, costs). The respective items are only shown if the
 * corresponding result is available.
 */
public class ResultTypeCombo {

	private final Collection<IndexFlow> flows;
	private final Collection<ImpactDescriptor> impacts;
	private final Collection<CostResultDescriptor> costs;
	private final Object initialSelection;
	private final EventHandler eventHandler;

	private ModelType selectedType = ModelType.FLOW;

	private ResultFlowCombo flowCombo;
	private ImpactCategoryViewer impactCombo;
	private CostResultViewer costCombo;

	private ResultTypeCombo(Builder builder) {
		initialSelection = builder.initialSelection;
		eventHandler = builder.eventHandler;

		var result = builder.result;
		this.flows = result.hasFlowResults()
			? result.getFlows()
			: null;
		this.impacts = result.hasImpactResults()
			? result.getImpacts()
			: null;

		// cost results
		if (!result.hasCostResults()) {
			this.costs = null;
		} else {
			var sr = (SimpleResult) result;
			var costs = new CostResultDescriptor();
			costs.forAddedValue = false;
			costs.name = M.Netcosts;
			var addedValue = new CostResultDescriptor();
			addedValue.forAddedValue = true;
			addedValue.name = M.AddedValue;

			this.costs = sr.totalCosts >= 0
				? Arrays.asList(costs, addedValue)
				: Arrays.asList(addedValue, costs);
		}
	}

	public static Builder on(IResult result) {
		return new Builder(result);
	}

	public void selectWithEvent(Object o) {
		if (o == null)
			return;
		boolean[] selection = null;
		if (o instanceof IndexFlow) {
			selectedType = ModelType.FLOW;
			flowCombo.select((IndexFlow) o);
			selection = new boolean[]{true, false, false};
		} else if (o instanceof ImpactDescriptor) {
			selectedType = ModelType.IMPACT_CATEGORY;
			impactCombo.select((ImpactDescriptor) o);
			selection = new boolean[]{false, true, false};
		} else if (o instanceof CostResultDescriptor) {
			selectedType = ModelType.CURRENCY;
			costCombo.select((CostResultDescriptor) o);
			selection = new boolean[]{false, false, true};
		}

		if (selection == null)
			return;
		AbstractComboViewer<?>[] combos = {
			flowCombo, impactCombo, costCombo,
		};
		for (int i = 0; i < selection.length; i++) {
			if (combos[i] != null) {
				combos[i].setEnabled(selection[i]);
			}
		}
	}

	/**
	 * Selects the first element (LCIA category, flow, costs) and activates the
	 * corresponding combo box.
	 */
	public void initWithEvent() {
		Collection<?> initial = impacts != null
			? impacts
			: flows != null
			? flows
			: costs;
		if (initial == null)
			return;
		initial.stream()
			.findFirst()
			.ifPresent(this::selectWithEvent);
	}

	public Object getSelection() {
		return switch (selectedType) {
			case FLOW -> flowCombo.getSelected();
			case IMPACT_CATEGORY -> impactCombo.getSelected();
			case CURRENCY -> costCombo.getSelected();
			default -> null;
		};
	}

	private void render(Composite comp, FormToolkit tk) {
		var initType = getType(initialSelection);
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
		var check = tk.createButton(comp, M.Flow, SWT.RADIO);
		check.setSelection(enabled);
		Controls.onSelect(check, _e -> {
			if (check.getSelection()) {
				flowCombo.setEnabled(true);
				selectedType = ModelType.FLOW;
				fireSelection();
			} else {
				flowCombo.setEnabled(false);
			}
		});

		flowCombo = new ResultFlowCombo(comp);
		flowCombo.setEnabled(enabled);
		flowCombo.setInput(flows);
		flowCombo.selectFirst();
		flowCombo.addSelectionChangedListener(_e -> fireSelection());
		if (enabled) {
			flowCombo.select((IndexFlow) initialSelection);
		}
	}

	private void initImpactCombo(FormToolkit tk, Composite comp) {
		boolean enabled = getType(initialSelection) == ModelType.IMPACT_CATEGORY;
		var check = tk.createButton(comp, M.ImpactCategory, SWT.RADIO);
		check.setSelection(enabled);
		Controls.onSelect(check, _e -> {
			if (check.getSelection()) {
				impactCombo.setEnabled(true);
				selectedType = ModelType.IMPACT_CATEGORY;
				fireSelection();
			} else {
				impactCombo.setEnabled(false);
			}
		});

		impactCombo = new ImpactCategoryViewer(comp);
		impactCombo.setEnabled(enabled);
		impactCombo.setInput(impacts);
		impactCombo.selectFirst();
		impactCombo.addSelectionChangedListener((val) -> fireSelection());
		if (enabled) {
			impactCombo.select((ImpactDescriptor) initialSelection);
		}
	}

	private void initCostCombo(FormToolkit tk, Composite comp) {
		boolean enabled = getType(initialSelection) == ModelType.CURRENCY;
		var check = tk.createButton(comp, M.CostCategory, SWT.RADIO);
		check.setSelection(enabled);
		Controls.onSelect(check, _e -> {
			if (check.getSelection()) {
				costCombo.setEnabled(true);
				selectedType = ModelType.CURRENCY;
				fireSelection();
			} else {
				costCombo.setEnabled(false);
			}
		});

		costCombo = new CostResultViewer(comp);
		costCombo.setEnabled(enabled);
		costCombo.setInput(costs);
		costCombo.selectFirst();
		costCombo.addSelectionChangedListener((val) -> fireSelection());
		if (enabled) {
			costCombo.select((CostResultDescriptor) initialSelection);
		}
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
		else if (o instanceof ImpactDescriptor)
			return ModelType.IMPACT_CATEGORY;
		else if (o instanceof CostResultDescriptor)
			return ModelType.CURRENCY;
		else
			return ModelType.UNKNOWN;
	}

	public interface EventHandler {

		void flowSelected(IndexFlow flow);

		void impactCategorySelected(ImpactDescriptor impact);

		void costResultSelected(CostResultDescriptor cost);

	}

	public static class Builder {

		private final IResult result;
		private Object initialSelection;
		private EventHandler eventHandler;

		private Builder(IResult result) {
			this.result = result;
		}

		public Builder withSelection(Object object) {
			this.initialSelection = object;
			return this;
		}

		public Builder withEventHandler(EventHandler handler) {
			this.eventHandler = handler;
			return this;
		}

		public ResultTypeCombo create(Composite comp, FormToolkit tk) {
			var combo = new ResultTypeCombo(this);
			combo.render(comp, tk);
			return combo;
		}
	}
}
