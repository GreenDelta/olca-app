package org.openlca.app.editors.lcia.geo;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.components.mapview.MapDialog;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.GeoJSON;
import org.openlca.geo.lcia.GeoFactorMerge;
import org.openlca.util.Strings;

import java.util.List;
import java.util.Map;

class GeoFactorDialog extends FormDialog {

	private final GeoPage page;
	private final List<ImpactFactor> factors;
	private boolean keepExisting = true;

	static void open(GeoPage page, List<ImpactFactor> factors) {
		new GeoFactorDialog(page, factors).open();
	}

	private GeoFactorDialog(GeoPage page, List<ImpactFactor> factors) {
		super(UI.shell());
		this.page = page;
		this.factors = factors;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Calculated factors");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(1000, 800);
	}

	@Override
	protected void createButtonsForButtonBar(Composite comp) {
		createButton(comp, IDialogConstants.OK_ID,
				"Add new factors", true);
		createButton(comp, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var body = UI.dialogBody(form.getForm(), tk);

		// merge strategy
		var radioComp = UI.composite(body, tk);
		UI.gridLayout(radioComp, 3);
		UI.label(radioComp, tk, "Merge factors: ");
		var keep = UI.radio(radioComp, tk, "Keep existing");
		keep.setSelection(keepExisting);
		Controls.onSelect(keep, $ -> keepExisting = keep.getSelection());
		var repl = UI.radio(radioComp, tk, "Replace existing");
		repl.setSelection(!keepExisting);
		Controls.onSelect(repl, $ -> keepExisting = !repl.getSelection());

		// factor table
		var table = Tables.createViewer(body,
				M.Flow, M.Category, M.Factor, M.Unit, M.Location);
		Tables.bindColumnWidths(table, 0.3, 0.25, 0.15, 0.15, 0.15);
		table.setLabelProvider(new FactorLabel(page));
		table.setInput(factors);

		var mapAction = Actions.create(
				"Show factors for flow", Icon.MAP.descriptor(), () -> openMap(table));
		Actions.bind(table, mapAction);
	}

	private void openMap(TableViewer table) {
		ImpactFactor factor = Viewers.getFirstSelected(table);
		if (factor == null || factor.flow == null)
			return;
		var flow = factor.flow;
		var coll = new FeatureCollection();
		for (var f : factors) {
			if (f.location == null)
				continue;
			var g = GeoJSON.unpack(f.location.geodata);
			if (g == null || g.isEmpty())
				continue;
			for (var feature : g.features) {
				feature.properties = Map.of("cf", f.value);
				coll.features.add(feature);
			}
		}
		var title = "Regionalized characterization factors for "
				+ Labels.name(flow);
		MapDialog.show(title, map -> map.addLayer(coll).fillScale("cf"));
	}

	@Override
	protected void okPressed() {
		var impact = page.editor.getModel();
		var merge = keepExisting
				? GeoFactorMerge.keepExisting(impact)
				: GeoFactorMerge.replaceExisting(impact);
		merge.doIt(factors);
		page.editor.setDirty(true);
		page.editor.emitEvent(page.editor.FACTORS_CHANGED_EVENT);
		page.editor.setActivePage("ImpactFactorPage");
		super.okPressed();
	}

	private static class FactorLabel extends LabelProvider
			implements ITableLabelProvider {

		private final ImpactCategory impact;

		FactorLabel(GeoPage page) {
			this.impact = page.editor.getModel();
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof ImpactFactor f))
				return null;
			return col == 0
					? Images.get(f.flow)
					: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ImpactFactor f))
				return null;
			return switch (col) {
				case 0 -> Labels.name(f.flow);
				case 1 -> Labels.category(f.flow);
				case 2 -> Numbers.format(f.value);
				case 3 -> unitOf(f);
				case 4 -> Labels.code(f.location);
				default -> null;
			};
		}

		private String unitOf(ImpactFactor f) {
			if (f.unit == null)
				return null;
			return Strings.notEmpty(impact.referenceUnit)
					? impact.referenceUnit + "/" + f.unit.name
					: "1/" + f.unit.name;
		}
	}
}
