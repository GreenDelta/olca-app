package org.openlca.app.results.analysis.sankey.actions;

import static org.eclipse.draw2d.PositionConstants.*;
import static org.openlca.app.components.graphics.figures.Connection.*;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ResultItemSelector;
import org.openlca.app.components.ResultItemSelector.SelectionHandler;
import org.openlca.app.results.analysis.sankey.SankeyConfig;
import org.openlca.app.util.Controls;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.UI;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.ResultItemOrder;

class SankeySelectionDialog extends FormDialog implements SelectionHandler {

	private final SankeyConfig config;
	private final ResultItemOrder items;

	SankeySelectionDialog(SankeyConfig config, ResultItemOrder items) {
		super(UI.shell());
		this.items = items;
		this.config = config;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var form = UI.header(mForm, M.SettingsOfTheSankeyDiagram);
		var body = UI.dialogBody(form, tk);
		UI.gridLayout(body, 2);
		ResultItemSelector.on(items)
				.withSelectionHandler(this)
				.withSelection(config.selection())
				.withoutCostSelector()  // no support for costs currently
				.create(body, tk);
		createCutoffSpinner(tk, body);
		createCountSpinner(tk, body);
		orientationsCombo(tk, body);
		connectionRoutersCombo(tk, body);
	}

	private void createCutoffSpinner(FormToolkit tk, Composite comp) {
		UI.label(comp, tk, M.MinContributionShare);
		var inner = UI.composite(comp, tk);
		UI.gridLayout(inner, 2, 10, 0);
		var spinner = UI.spinner(inner, tk);
		spinner.setIncrement(100);
		spinner.setMinimum(0);
		spinner.setMaximum(100000);
		spinner.setDigits(3);
		spinner.setSelection((int) (config.cutoff() * 100000));
		spinner.addModifyListener(
				e -> config.setCutoff(spinner.getSelection() / 100000d));
		tk.adapt(spinner);
		UI.label(inner, tk, "%");
	}

	private void createCountSpinner(FormToolkit tk, Composite comp) {
		UI.label(comp, tk, M.MaxNumberOfProcesses);
		var inner = UI.composite(comp, tk);
		UI.gridLayout(inner, 2, 10, 0);
		var spinner = UI.spinner(inner, tk);
		spinner.setIncrement(1);
		spinner.setMinimum(1);
		spinner.setMaximum(items.techFlows().size());
		spinner.setDigits(0);
		spinner.setSelection(config.maxCount());
		spinner.addModifyListener(e -> config.setMaxCount(spinner.getSelection()));
		tk.adapt(spinner);
		tk.createLabel(inner, "");
	}

	private void orientationsCombo(FormToolkit tk, Composite comp) {
		var combo = UI.labeledCombo(comp, tk, M.Orientation);
		UI.gridData(combo, true, false);
		var orientations = new int[]{
				NORTH,
				SOUTH,
				WEST,
				EAST
		};
		for (var orientation : orientations) {
			combo.add(orientationOf(orientation));
		}
		combo.select(
				ArrayUtils.indexOf(orientations, config.orientation()));
		Controls.onSelect(combo, e -> {
			var orientation = orientations[combo.getSelectionIndex()];
			config.setOrientation(orientation);
		});
	}

	private void connectionRoutersCombo(FormToolkit tk, Composite comp) {
		var combo = UI.labeledCombo(comp, tk, M.Connections);
		UI.gridData(combo, true, false);
		var connectionRouters = new String[]{
				ROUTER_NULL,
				ROUTER_CURVE
		};
		for (var router : connectionRouters) {
			combo.add(router);
		}

		combo.select(
				ArrayUtils.indexOf(connectionRouters, config.connectionRouter()));
		Controls.onSelect(combo, e -> {
			var router = connectionRouters[combo.getSelectionIndex()];
			config.setConnectionRouter(router);
		});
	}

	private static String orientationOf(int orientation) {
		return switch (orientation) {
			case NORTH -> M.North;
			case SOUTH -> M.South;
			case WEST -> M.West;
			case EAST -> M.East;
			default -> "";
		};
	}

	@Override
	public void onFlowSelected(EnviFlow flow) {
		config.setSelection(flow);
	}

	@Override
	public void onImpactSelected(ImpactDescriptor impact) {
		config.setSelection(impact);
	}

	@Override
	public void onCostsSelected(CostResultDescriptor cost) {
		config.setSelection(cost);
	}

}
