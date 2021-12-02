package org.openlca.app.results.contributions.locations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.util.Controls;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.app.viewers.combo.CostResultViewer;
import org.openlca.app.viewers.combo.FlowViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.IResult;
import org.openlca.core.results.SimpleResult;

import gnu.trove.set.hash.TLongHashSet;

/**
 * This is in principle the same as the ResultTypeCombo but with plain flow
 * descriptors instead of index flows (because index flows may are regionalized
 * but we want to see the contributions of the regions in this page).
 */
class Combo {

	private ModelType selectedType = ModelType.FLOW;

	private List<FlowDescriptor> flows;
	private List<ImpactDescriptor> impacts;
	private List<CostResultDescriptor> costs;
	private Object initialSelection;

	private Consumer<Object> handler;
	private FlowViewer flowCombo;
	private ImpactCategoryViewer impactCombo;
	private CostResultViewer costCombo;

	public static Builder on(IResult r) {
		Combo c = new Combo();
		c.flows = new ArrayList<>();
		TLongHashSet flowIDs = new TLongHashSet();
		for (var f : r.getFlows()) {
			if (f.flow() == null || flowIDs.contains(f.flow().id))
				continue;
			flowIDs.add(f.flow().id);
			c.flows.add(f.flow());
		}

		// add LCIA categories
		if (r.hasImpacts()) {
			c.impacts = r.getImpacts();
		}

		// add cost / added value selection
		if (r.hasCosts() && (r instanceof SimpleResult)) {
			SimpleResult sr = (SimpleResult) r;
			CostResultDescriptor d1 = new CostResultDescriptor();
			d1.forAddedValue = false;
			d1.name = M.Netcosts;
			CostResultDescriptor d2 = new CostResultDescriptor();
			d2.forAddedValue = true;
			d2.name = M.AddedValue;
			c.costs = sr.totalCosts() >= 0
					? Arrays.asList(d1, d2)
					: Arrays.asList(d2, d1);
		}

		return new Builder(c);
	}

	private Combo() {
	}

	public void selectWithEvent(Object o) {
		if (o == null)
			return;
		AbstractComboViewer<?>[] combos = {
				flowCombo, impactCombo, costCombo,
		};
		boolean[] selection = null;
		if (o instanceof FlowDescriptor) {
			selectedType = ModelType.FLOW;
			flowCombo.select((FlowDescriptor) o);
			selection = new boolean[] { true, false, false };
		} else if (o instanceof ImpactDescriptor) {
			selectedType = ModelType.IMPACT_CATEGORY;
			impactCombo.select((ImpactDescriptor) o);
			selection = new boolean[] { false, true, false };
		} else if (o instanceof CostResultDescriptor) {
			selectedType = ModelType.CURRENCY;
			costCombo.select((CostResultDescriptor) o);
			selection = new boolean[] { false, false, true };
		}

		if (selection == null)
			return;
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

		if (impacts != null) {
			ImpactDescriptor impact = impacts.stream()
					.findFirst().orElse(null);
			if (impact != null) {
				selectWithEvent(impact);
				return;
			}
		}

		if (flows != null) {
			FlowDescriptor flow = flows.stream()
					.findFirst().orElse(null);
			if (flow != null) {
				selectWithEvent(flow);
				return;
			}
		}

		if (costs != null && !costs.isEmpty()) {
			selectWithEvent(costs.iterator().hasNext());
		}
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
		Controls.onSelect(check, _e -> {
			if (check.getSelection()) {
				flowCombo.setEnabled(true);
				selectedType = ModelType.FLOW;
				fire(flowCombo.getSelected());
			} else {
				flowCombo.setEnabled(false);
			}
		});

		flowCombo = new FlowViewer(comp);
		flowCombo.setEnabled(enabled);
		flowCombo.setInput(flows);
		flowCombo.selectFirst();
		flowCombo.addSelectionChangedListener(f -> {
			fire(f);
			check.setSelection(true);
		});
		if (enabled) {
			flowCombo.select((FlowDescriptor) initialSelection);
		}
	}

	private void initImpactCombo(FormToolkit tk, Composite comp) {
		boolean enabled = getType(initialSelection) == ModelType.IMPACT_CATEGORY;
		Button check = tk.createButton(comp, M.ImpactCategory, SWT.RADIO);
		check.setSelection(enabled);
		Controls.onSelect(check, _e -> {
			if (check.getSelection()) {
				impactCombo.setEnabled(true);
				selectedType = ModelType.IMPACT_CATEGORY;
				fire(impactCombo.getSelected());
			} else {
				impactCombo.setEnabled(false);
			}
		});

		impactCombo = new ImpactCategoryViewer(comp);
		impactCombo.setEnabled(enabled);
		impactCombo.setInput(impacts);
		impactCombo.selectFirst();
		impactCombo.addSelectionChangedListener(imp -> {
			fire(imp);
			check.setSelection(true);
		});
		if (enabled) {
			impactCombo.select((ImpactDescriptor) initialSelection);
		}
	}

	private void initCostCombo(FormToolkit tk, Composite comp) {
		boolean enabled = getType(initialSelection) == ModelType.CURRENCY;
		Button check = tk.createButton(comp, M.CostCategory, SWT.RADIO);
		check.setSelection(enabled);
		Controls.onSelect(check, _e -> {
			if (check.getSelection()) {
				costCombo.setEnabled(true);
				selectedType = ModelType.CURRENCY;
				fire(costCombo.getSelected());
			} else {
				costCombo.setEnabled(false);
			}
		});

		costCombo = new CostResultViewer(comp);
		costCombo.setEnabled(enabled);
		costCombo.setInput(costs);
		costCombo.selectFirst();
		costCombo.addSelectionChangedListener(c -> {
			fire(c);
			check.setSelection(true);
		});
		if (enabled) {
			costCombo.select((CostResultDescriptor) initialSelection);
		}
	}

	private void fire(Object selection) {
		if (handler != null) {
			handler.accept(selection);
		}
	}

	private ModelType getType(Object o) {
		if (o instanceof FlowDescriptor)
			return ModelType.FLOW;
		else if (o instanceof ImpactDescriptor)
			return ModelType.IMPACT_CATEGORY;
		else if (o instanceof CostResultDescriptor)
			return ModelType.CURRENCY;
		else
			return ModelType.UNKNOWN;
	}

	/**
	 * Builder class for the initialization of the widgets.
	 */
	public static class Builder {

		private Combo selection;

		private Builder(Combo selection) {
			this.selection = selection;
		}

		public Builder withSelection(Object item) {
			selection.initialSelection = item;
			return this;
		}

		public Builder onSelected(Consumer<Object> handler) {
			selection.handler = handler;
			return this;
		}

		public Combo create(Composite comp, FormToolkit tk) {
			selection.render(comp, tk);
			return selection;
		}
	}
}

