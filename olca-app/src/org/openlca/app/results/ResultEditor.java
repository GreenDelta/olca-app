package org.openlca.app.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.util.Labels;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.SimpleResult;
import org.openlca.util.Strings;

public abstract class ResultEditor<T extends SimpleResult> extends FormEditor {

	public T result;
	public CalculationSetup setup;
	public DQResult dqResult;

	// cached data for increasing performance in the result views
	private List<IndexFlow> _flows;
	private final HashMap<IndexFlow, String> _flowNames = new HashMap<>();
	private final HashMap<IndexFlow, String> _flowUnits = new HashMap<>();
	private final HashMap<IndexFlow, String> _flowCategories = new HashMap<>();
	private List<ImpactCategoryDescriptor> _impacts;

	/**
	 * Returns the sorted list of flows from the result. As sorting of big flow
	 * lists can really take some time, it is recommended to always use this method
	 * in the editor pages to get the flow list.
	 */
	public List<IndexFlow> flows() {
		if (_flows != null)
			return _flows;
		_flows = new ArrayList<>();
		if (result.flowIndex != null) {
			result.flowIndex.each((i, f) -> _flows.add(f));
			Collections.sort(_flows, (f1, f2) -> {
				// this also caches the flow names
				String n1 = name(f1);
				String n2 = name(f2);
				return Strings.compare(n1, n2);
			});
		}
		return _flows;
	}

	/** Get the list of sorted LCIA categories from the result. */
	public List<ImpactCategoryDescriptor> impacts() {

		if (_impacts != null)
			return _impacts;
		if (result.impactIndex == null) {
			_impacts = Collections.emptyList();
			return _impacts;
		}

		_impacts = result.getImpacts().stream()
				.sorted((i1, i2) -> Strings.compare(i1.name, i2.name))
				.collect(Collectors.toList());
		return _impacts;
	}

	/**
	 * Returns the display name of the given flow. The names are cached in this
	 * editor in order to increase performance.
	 */
	public String name(IndexFlow flow) {
		return label(flow, _flowNames, () -> {
			if (flow.flow.flowType != FlowType.ELEMENTARY_FLOW)
				return Labels.getDisplayName(flow.flow);
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
			return Labels.getRefUnit(flow);
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
