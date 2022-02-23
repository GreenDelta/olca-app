package org.openlca.app.results.contributions.locations;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.trees.TreeClipboard;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.Location;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.LocationResult;

/**
 * Shows the contributions of the locations in the product system to an analysis
 * result.
 */
public class LocationPage extends FormPage {

	final ContributionResult result;
	private final LocationResult locations;
	private final CalculationSetup setup;

	private Combo combos;
	private TreeViewer tree;
	private TreeLabel label;

	// private LocationMap map;
	private ResultMap map;
	boolean skipZeros = true;
	double cutoff = 0.01;

	public LocationPage(FormEditor editor,
			ContributionResult result, CalculationSetup setup) {
		super(editor, "analysis.MapPage", M.Locations);
		this.setup = setup;
		this.result = result;
		this.locations = new LocationResult(result, Database.get());
	}

	public Object getSelection() {
		return combos == null ? null : combos.getSelection();
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform,
				Labels.name(setup.target()),
				Images.get(result));
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		createCombos(body, tk);
		SashForm sash = new SashForm(body, SWT.VERTICAL);
		UI.gridData(sash, true, true);
		tk.adapt(sash);
		createTree(sash, tk);
		map = ResultMap.on(sash, tk);
		form.reflow(true);
		refreshSelection();
	}

	private void createCombos(Composite body, FormToolkit tk) {

		Composite outer = tk.createComposite(body);
		UI.gridLayout(outer, 2, 5, 0);
		Composite comboComp = tk.createComposite(outer);
		UI.gridLayout(comboComp, 2);
		combos = Combo.on(result)
				.onSelected(this::onSelected)
				.withSelection(result.getFlows().iterator().next())
				.create(comboComp, tk);

		Composite cutoffComp = tk.createComposite(outer);
		UI.gridLayout(cutoffComp, 1, 0, 0);
		GridData gd = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
		cutoffComp.setLayoutData(gd);

		Composite checkComp = tk.createComposite(cutoffComp);
		tk.createLabel(checkComp, M.DontShowSmallerThen);
		Spinner spinner = new Spinner(checkComp, SWT.BORDER);
		spinner.setValues(1, 0, 100, 0, 1, 10);
		tk.adapt(spinner);
		tk.createLabel(checkComp, "%");
		Controls.onSelect(spinner, e -> {
			cutoff = (spinner.getSelection()) / 100d;
			refreshSelection();
		});

		UI.gridLayout(checkComp, 5);
		Button zeroCheck = UI.formCheckBox(checkComp, tk, M.ExcludeZeroEntries);
		zeroCheck.setSelection(skipZeros);
		Controls.onSelect(zeroCheck, event -> {
			skipZeros = zeroCheck.getSelection();
			refreshSelection();
		});
	}

	private void createTree(Composite body, FormToolkit tk) {
		Section section = UI.section(body, tk, M.ContributionTreeLocations);
		UI.gridData(section, true, true);
		Composite comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);
		label = new TreeLabel();
		String[] labels = { M.Location, M.Amount, M.Unit };
		tree = Trees.createViewer(comp, labels, label);
		tree.setContentProvider(new TreeContentProvider(this));
		Trees.bindColumnWidths(tree.getTree(), 0.4, 0.3, 0.3);

		// tree actions
		Action onOpen = Actions.onOpen(() -> {
			Object obj = Viewers.getFirstSelected(tree);
			if (obj == null)
				return;
			if (obj instanceof Contribution) {
				Contribution<?> c = (Contribution<?>) obj;
				if (c.item instanceof RootDescriptor) {
					App.open((RootDescriptor) c.item);
				} else if (c.item instanceof RootEntity) {
					App.open((RootEntity) c.item);
				}
			}
		});
		Actions.bind(tree, onOpen, TreeClipboard.onCopy(tree));
		Trees.onDoubleClick(tree, e -> onOpen.run());
		tree.getTree().getColumns()[1].setAlignment(SWT.RIGHT);

	}

	// the map can be a bit lazy. thus it can call this method to force an
	// update of the current results
	void refreshSelection() {
		if (combos != null) {
			combos.selectWithEvent(combos.getSelection());
		}
	}

	private void onSelected(Object obj) {
		label.update(obj);
		if (obj instanceof FlowDescriptor) {
			FlowDescriptor f = (FlowDescriptor) obj;
			update(locations.getContributions(f));
			return;
		}
		if (obj instanceof ImpactDescriptor) {
			var i = (ImpactDescriptor) obj;
			update(locations.getContributions(i));
			return;
		}
		if (obj instanceof CostResultDescriptor) {
			var c = (CostResultDescriptor) obj;
			if (c.forAddedValue) {
				update(locations.getAddedValueContributions());
			} else {
				update(locations.getNetCostsContributions());
			}
		}
	}

	private void update(List<Contribution<Location>> items) {
		List<Contribution<Location>> sorted = items.stream()
				.filter(c -> c.amount != 0)
				.sorted((c1, c2) -> Double.compare(c2.amount, c1.amount))
				.collect(Collectors.toList());
		if (tree != null) {
			tree.setInput(sorted);
		}
		if (map != null) {
			map.update(getSelection(), sorted);
		}
	}
}
