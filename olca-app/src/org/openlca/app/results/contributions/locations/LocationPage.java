package org.openlca.app.results.contributions.locations;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.trees.TreeClipboard;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.model.Location;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.LocationResult;
import org.openlca.util.Strings;

/**
 * Shows the contributions of the locations in the product system to an analysis
 * result.
 */
public class LocationPage extends FormPage {

	final ResultEditor editor;
	private final LocationResult locations;

	private Combo combos;
	private TreeViewer tree;
	private TreeLabel label;
	private ResultMap map;
	boolean skipZeros = true;
	double cutoff = 0.01;

	public LocationPage(ResultEditor editor) {
		super(editor, "analysis.MapPage", M.Locations);
		this.editor = editor;
		this.locations = new LocationResult(editor.result, Database.get());
	}

	public Object getSelection() {
		return combos == null ? null : combos.getSelection();
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.header(mform,
				Labels.name(editor.setup.target()),
				Icon.ANALYSIS_RESULT.get());
		var tk = mform.getToolkit();
		var body = UI.body(form, tk);
		createCombos(body, tk);
		var sash = new SashForm(body, SWT.VERTICAL);
		UI.gridData(sash, true, true);
		tk.adapt(sash);
		createTree(sash, tk);
		map = ResultMap.on(sash, tk);
		form.reflow(true);
		refreshSelection();
	}

	private void createCombos(Composite body, FormToolkit tk) {

		Composite outer = UI.composite(body, tk);
		UI.gridLayout(outer, 2, 5, 0);
		Composite comboComp = UI.composite(outer, tk);
		UI.gridLayout(comboComp, 2);
		combos = Combo.on(editor)
				.onSelected(this::onSelected)
				.create(comboComp, tk);
		if (editor.items.enviFlows().size() > 0) {
			combos.selectWithEvent(editor.items.enviFlows().get(0));
		}

		Composite cutoffComp = UI.composite(outer, tk);
		UI.gridLayout(cutoffComp, 1, 0, 0);
		GridData gd = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
		cutoffComp.setLayoutData(gd);

		Composite checkComp = UI.composite(cutoffComp, tk);
		UI.gridLayout(checkComp, 5);

		UI.label(checkComp, tk, M.DontShowSmallerThen);
		Spinner spinner = UI.spinner(checkComp, tk, SWT.BORDER);
		spinner.setValues(1, 0, 100, 0, 1, 10);
		UI.label(checkComp, tk, "%");
		Controls.onSelect(spinner, e -> {
			cutoff = (spinner.getSelection()) / 100d;
			refreshSelection();
		});

		Button zeroCheck = UI.labeledCheckbox(checkComp, tk, M.ExcludeZeroEntries);
		zeroCheck.setSelection(skipZeros);
		Controls.onSelect(zeroCheck, event -> {
			skipZeros = zeroCheck.getSelection();
			refreshSelection();
		});
	}

	private void createTree(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, M.ContributionTreeLocations);
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);
		label = new TreeLabel();
		String[] labels = {M.Location, M.Amount, M.Unit};
		tree = Trees.createViewer(comp, labels, label);
		tree.setContentProvider(new TreeContentProvider(this));
		Trees.bindColumnWidths(tree.getTree(), 0.4, 0.3, 0.3);

		// tree actions
		var onOpen = Actions.onOpen(() -> {
			Object obj = Viewers.getFirstSelected(tree);
			if (obj == null)
				return;
			if (obj instanceof Contribution<?> c) {
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
		if (obj instanceof FlowDescriptor f) {
			update(locations.getContributions(f));
			return;
		}
		if (obj instanceof ImpactDescriptor i) {
			update(locations.getContributions(i));
			return;
		}
		if (obj instanceof CostResultDescriptor c) {
			if (c.forAddedValue) {
				update(locations.getAddedValueContributions());
			} else {
				update(locations.getNetCostsContributions());
			}
		}
	}

	private void update(List<Contribution<Location>> items) {
		var sorted = items.stream()
				.filter(this::applyFilter)
				.sorted((c1, c2) -> {
					int c = Double.compare(c2.amount, c1.amount);
					return c == 0
							? Strings.compare(Labels.name(c1.item), Labels.name(c2.item))
							: c;
				})
				.collect(Collectors.toList());
		if (tree != null) {
			tree.setInput(sorted);
		}
		if (map != null) {
			map.update(getSelection(), sorted);
		}
	}

	boolean applyFilter(Contribution<?> c) {
		if (c == null || c.item == null)
			return false;
		if (c.amount == 0 && skipZeros)
			return false;
		if (cutoff > 0)
			return Math.abs(c.share) >= cutoff;
		return true;
	}
}
