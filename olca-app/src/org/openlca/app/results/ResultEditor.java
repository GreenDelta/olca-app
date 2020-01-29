package org.openlca.app.results;

import java.util.HashMap;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.util.Labels;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.model.FlowType;
import org.openlca.core.results.ContributionResult;

public abstract class ResultEditor<T extends ContributionResult> extends FormEditor {

	public T result;
	public CalculationSetup setup;
	public DQResult dqResult;

	// cached data for increasing performance in the result views
	private final HashMap<IndexFlow, String> _flowNames = new HashMap<>();
	private final HashMap<IndexFlow, String> _flowUnits = new HashMap<>();
	private final HashMap<IndexFlow, String> _flowCategories = new HashMap<>();

	/**
	 * Returns the display name of the given flow. The names are cached in this
	 * editor in order to increase performance.
	 */
	public String name(IndexFlow flow) {
		return label(flow, _flowNames, () -> {
			if (flow.flow.flowType != FlowType.ELEMENTARY_FLOW)
				return Labels.name(flow.flow);
			String name = flow.flow.name;
			if (flow.location != null) {
				name += " - " + flow.location.code;
			}
			return name;
		});
	}

	/** Returns the name of the reference unit of the given flow. */
	public String unit(IndexFlow flow) {
		return label(flow, _flowUnits, () -> {
			return Labels.refUnit(flow);
		});
	}

	/** Returns the category path of the given flow. */
	public String category(IndexFlow flow) {
		return label(flow, _flowCategories, () -> {
			return Labels.category(flow.flow);
		});
	}

	private String label(IndexFlow flow,
			HashMap<IndexFlow, String> cache,
			Supplier<String> fn) {
		if (flow == null)
			return "";
		String v = cache.get(flow);
		if (v != null)
			return v;
		v = fn.get();
		if (v == null) {
			v = "";
		}
		cache.put(flow, v);
		return v;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
		SaveProcessDialog.open(this);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public void setFocus() {
	}

}
