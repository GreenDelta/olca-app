package org.openlca.app.results.contributions.locations;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.ResultTypeSelection;
import org.openlca.app.db.Cache;
import org.openlca.app.util.UI;
import org.openlca.core.database.EntityCache;
import org.openlca.core.results.ContributionResultProvider;

/**
 * Shows the contributions of the locations in the product system to an analysis
 * result.
 */
public class LocationPage extends FormPage {

	private EntityCache cache = Cache.getEntityCache();
	ContributionResultProvider<?> result;

	private ResultTypeSelection combos;
	private LocationTree tree;
	private LocationMap map;
	private boolean showMap;

	public LocationPage(FormEditor editor, ContributionResultProvider<?> result) {
		this(editor, result, true);
	}

	public LocationPage(FormEditor editor, ContributionResultProvider<?> result,
			boolean showMap) {
		super(editor, "analysis.MapPage", M.Locations);
		this.showMap = showMap;
		this.result = result;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, M.Locations);
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		createCombos(body, tk);
		createTree(body, tk);
		if (showMap) {
			map = LocationMap.create(this, body, tk);
		}
		form.reflow(true);
		refreshSelection();
	}

	private void createCombos(Composite body, FormToolkit tk) {
		Composite composite = tk.createComposite(body);
		UI.gridLayout(composite, 2);
		combos = ResultTypeSelection.on(result, cache)
				.withEventHandler(new SelectionHandler(this))
				.withSelection(result.getFlowDescriptors().iterator().next())
				.create(composite, tk);
	}

	private void createTree(Composite body, FormToolkit tk) {
		Section section = UI.section(body, tk, M.ContributionTreeLocations);
		UI.gridData(section, true, true);
		Composite composite = UI.sectionClient(section, tk);
		UI.gridLayout(composite, 1);
		tree = new LocationTree(composite, showMap);
	}

	// the map can be a bit lazy. thus it can call this method to force an
	// update of the current results
	void refreshSelection() {
		if (combos != null) {
			combos.selectWithEvent(combos.getSelection());
		}
	}

	void setInput(List<LocationItem> items, String unit) {
		if (tree != null) {
			tree.setInput(items, unit);
		}
		if (map != null && showMap) {
			map.setInput(items);
		}
	}
}
