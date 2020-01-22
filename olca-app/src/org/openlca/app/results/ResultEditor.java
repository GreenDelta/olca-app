package org.openlca.app.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.SimpleResult;
import org.openlca.util.Strings;

public abstract class ResultEditor<T extends SimpleResult> extends FormEditor {

	public T result;
	public CalculationSetup setup;
	public DQResult dqResult;

	private List<IndexFlow> _flows;
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
				if (f1.flow == null || f2.flow == null)
					return 0;
				int c = Strings.compare(f1.flow.name, f2.flow.name);
				if (c != 0)
					return c;
				String loc1 = f1.location != null
						? f1.location.code
						: null;
				String loc2 = f2.location != null
						? f2.location.code
						: null;
				return Strings.compare(loc1, loc2);
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
