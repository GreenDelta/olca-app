package org.openlca.app.components;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.util.Controls;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.app.viewers.combo.CostResultViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.ResultItemView;

/**
 * Multiple combo boxes that allow to switch between different result types
 * (flows, LCIA categories, costs). The respective items are only shown if the
 * corresponding result is available.
 */
public class ResultItemSelector {

	private final Collection<EnviFlow> flows;
	private final Collection<ImpactDescriptor> impacts;
	private final Collection<CostResultDescriptor> costs;
	private final Object initialSelection;
	private final SelectionHandler eventHandler;

	private ModelType selectedType = ModelType.FLOW;

	private Button flowCheck;
	private ResultFlowCombo flowCombo;

	private Button impactCheck;
	private ImpactCategoryViewer impactCombo;

	private Button costCheck;
	private CostResultViewer costCombo;

	private ResultItemSelector(Builder builder) {
		initialSelection = builder.initialSelection;
		eventHandler = builder.eventHandler;
		var resultItems = builder.resultItems;

		var result = builder.resultItems;
		this.flows = resultItems.hasEnviFlows()
			? result.enviFlows()
			: null;
		this.impacts = resultItems.hasImpacts()
			? result.impacts()
			: null;

		// cost results
		if (!resultItems.hasCosts()) {
			this.costs = null;
		} else {
			var costs = new CostResultDescriptor();
			costs.forAddedValue = false;
			costs.name = M.Netcosts;
			var addedValue = new CostResultDescriptor();
			addedValue.forAddedValue = true;
			addedValue.name = M.AddedValue;
			this.costs = Arrays.asList(costs, addedValue);
		}
	}

	public static Builder on(ResultItemView resultItems) {
		return new Builder(resultItems);
	}

	public void selectWithEvent(Object o) {
		if (o == null)
			return;
		boolean[] selection = null;
		if (o instanceof EnviFlow) {
			selectedType = ModelType.FLOW;
			flowCombo.select((EnviFlow) o);
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
		Button[] checks = {
			flowCheck, impactCheck, costCheck,
		};
		for (int i = 0; i < selection.length; i++) {
			if (combos[i] != null) {
				combos[i].setEnabled(selection[i]);
			}
			var check = checks[i];
			if (check != null && !check.getSelection()) {
				check.setSelection(selection[i]);
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
		flowCheck = tk.createButton(comp, M.Flow, SWT.RADIO);
		flowCheck.setSelection(enabled);
		Controls.onSelect(flowCheck, _e -> {
			if (flowCheck.getSelection()) {
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
			flowCombo.select((EnviFlow) initialSelection);
		}
	}

	private void initImpactCombo(FormToolkit tk, Composite comp) {
		boolean enabled = getType(initialSelection) == ModelType.IMPACT_CATEGORY;
		impactCheck = tk.createButton(comp, M.ImpactCategory, SWT.RADIO);
		impactCheck.setSelection(enabled);
		Controls.onSelect(impactCheck, _e -> {
			if (impactCheck.getSelection()) {
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
		costCheck = tk.createButton(comp, M.CostCategory, SWT.RADIO);
		costCheck.setSelection(enabled);
		Controls.onSelect(costCheck, _e -> {
			if (costCheck.getSelection()) {
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
				eventHandler.onFlowSelected(flowCombo.getSelected());
				break;
			case IMPACT_CATEGORY:
				eventHandler.onImpactSelected(impactCombo.getSelected());
				break;
			case CURRENCY:
				eventHandler.onCostsSelected(costCombo.getSelected());
				break;
			default:
				break;
		}
	}

	private ModelType getType(Object o) {
		if (o instanceof EnviFlow)
			return ModelType.FLOW;
		else if (o instanceof ImpactDescriptor)
			return ModelType.IMPACT_CATEGORY;
		else if (o instanceof CostResultDescriptor)
			return ModelType.CURRENCY;
		else
			return ModelType.UNKNOWN;
	}

	public interface SelectionHandler {

		void onFlowSelected(EnviFlow flow);

		void onImpactSelected(ImpactDescriptor impact);

		void onCostsSelected(CostResultDescriptor cost);

	}

	public static class Builder {

		private final ResultItemView resultItems;
		private Object initialSelection;
		private SelectionHandler eventHandler;

		private Builder(ResultItemView indexView) {
			this.resultItems = indexView;
		}

		public Builder withSelection(Object object) {
			this.initialSelection = object;
			return this;
		}

		public Builder withSelectionHandler(SelectionHandler handler) {
			this.eventHandler = handler;
			return this;
		}

		public ResultItemSelector create(Composite comp, FormToolkit tk) {
			var combo = new ResultItemSelector(this);
			combo.render(comp, tk);
			return combo;
		}
	}
}
