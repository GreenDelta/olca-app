package org.openlca.app.results.impacts;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.ContributionCutoff;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.util.*;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.trees.TreeClipboard;
import org.openlca.app.viewers.trees.TreeClipboard.Provider;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.results.LcaResult;
import org.openlca.core.results.ResultItemOrder;
import org.openlca.util.Strings;

public class ImpactTreePage extends FormPage {

	private final ResultEditor editor;
	private final CalculationSetup setup;
	private final LcaResult result;
	private final DQResult dqResult;

	private TreeViewer viewer;
	private ContributionCutoff cutoff;

	final ResultItemOrder items;
	boolean flowsFirst = true;

	public ImpactTreePage(ResultEditor editor) {
		super(editor, "ImpactTreePage", M.ImpactAnalysis);
		this.editor = editor;
		this.result = editor.result;
		this.setup = editor.setup;
		this.dqResult = editor.dqResult;
		this.items = editor.items;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm,
				Labels.name(setup.target()),
				Icon.ANALYSIS_RESULT.get());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		var section = UI.section(body, tk,
				M.ImpactAnalysis + ": " + Labels.name(setup.impactMethod()));
		UI.gridData(section, true, true);
		var comp = UI.composite(section, tk);
		section.setClient(comp);
		UI.gridLayout(comp, 1);
		createOptions(comp, tk);
		createTree(comp, tk);
		cutoff.register(viewer);
		form.reflow(true);
	}

	private void createOptions(Composite parent, FormToolkit tk) {
		var comp = UI.composite(parent, tk);
		UI.gridLayout(comp, 5);
		UI.label(comp, tk, "Sub-group by:");
		var flowCheck = UI.radio(comp, tk, M.Flows);
		flowCheck.setSelection(flowsFirst);
		var processCheck = UI.radio(comp, tk, M.Processes);
		processCheck.setSelection(!flowsFirst);
		Controls.onSelect(flowCheck, e -> {
			flowsFirst = flowCheck.getSelection();
			setInput();
		});
		UI.label(comp, tk, " | ");
		cutoff = ContributionCutoff.create(comp, tk);
	}

	private void setInput() {
		viewer.setInput(TreeItem.rootsOf(result, items.impacts()));
	}

	private void createTree(Composite comp, FormToolkit tk) {
		String[] columns = {
				M.Name,
				M.Category,
				M.InventoryResult,
				M.ImpactFactor,
				M.ImpactResult};
		if (DQUI.displayExchangeQuality(dqResult)) {
			columns = DQUI.appendTableHeaders(columns,
					dqResult.setup.exchangeSystem);
		}
		var label = new TreeLabel(editor.dqResult);
		viewer = Trees.createViewer(comp, columns, label);
		viewer.setContentProvider(new TreeContent(this));
		tk.adapt(viewer.getTree(), false, false);
		tk.paintBordersFor(viewer.getTree());

		var onOpen = Actions.onOpen(this::onOpen);
		var onCopy = TreeClipboard.onCopy(viewer, new ClipboardProvider(label));
		Actions.bind(viewer, onOpen, onCopy);
		Trees.onDoubleClick(viewer, e -> onOpen.run());
		createColumnSorters(label);
		double[] widths = {.3, .25, .15, .15, .15};
		if (DQUI.displayExchangeQuality(dqResult)) {
			widths = DQUI.adjustTableWidths(
					widths, dqResult.setup.exchangeSystem);
		}
		viewer.getTree().getColumns()[2].setAlignment(SWT.RIGHT);
		viewer.getTree().getColumns()[3].setAlignment(SWT.RIGHT);
		viewer.getTree().getColumns()[4].setAlignment(SWT.RIGHT);
		Trees.bindColumnWidths(viewer.getTree(), widths);
		setInput();
	}

	private void onOpen() {
		TreeItem item = Viewers.getFirstSelected(viewer);
		if (item == null)
			return;
		if (item.isRoot()) {
			App.open(item.impact());
		} else if (item.isEnviItem()) {
			App.open(item.enviFlow().flow());
		} else if (item.isTechItem()) {
			App.open(item.techFlow().provider());
		}
	}

	private void createColumnSorters(TreeLabel p) {
		Viewers.sortByLabels(viewer, p, 0, 1);
		Viewers.sortByDouble(viewer, item -> ((TreeItem) item).inventoryResult(), 2);
		Viewers.sortByDouble(viewer, item -> ((TreeItem) item).impactFactor(), 3);
		Viewers.sortByDouble(viewer, item -> ((TreeItem) item).impactResult(), 4);
		if (!DQUI.displayExchangeQuality(dqResult))
			return;
		for (int i = 0; i < dqResult.setup.exchangeSystem.indicators.size(); i++) {
			Viewers.sortByDouble(viewer, p, i + 5);
		}
	}

	private record ClipboardProvider(TreeLabel label) implements Provider {

		@Override
		public int columns() {
			return 8;
		}

		@Override
		public String getHeader(int col) {
			return switch (col) {
				case 0 -> M.Name;
				case 1 -> M.Category;
				case 2 -> M.InventoryResult;
				case 3, 5, 7 -> M.Unit;
				case 4 -> M.ImpactFactor;
				case 6 -> M.ImpactResult;
				default -> "";
			};
		}

		@Override
		public String getLabel(org.eclipse.swt.widgets.TreeItem widget, int col) {
			if (!(widget.getData() instanceof TreeItem item))
				return "";

			var inventoryValue = "";
			var inventoryUnit = "";
			var impactFactorUnit = "";
			if (item.isEnviItem()) {
				inventoryValue = Double.toString(item.inventoryResult());
				inventoryUnit = Labels.refUnit(item.enviFlow());
				var impactUnit = item.impact().referenceUnit;
				impactFactorUnit = Strings.notEmpty(impactUnit)
						? impactUnit + "/" + inventoryUnit
						: "1/" + inventoryUnit;
			} else if (item.isTechItem() && item.isLeaf()) {
				inventoryValue = Double.toString(item.inventoryResult());
				inventoryUnit = Labels.refUnit(item.parent().enviFlow());
			}

			return switch (col) {
				case 0, 1 -> label.getText(item, col);
				case 2 -> inventoryValue;
				case 3 -> inventoryUnit;
				case 4 -> item.isEnviItem()
						? Double.toString(item.impactFactor())
						: "";
				case 5 -> impactFactorUnit;
				case 6 -> Double.toString(item.impactResult());
				case 7 -> item.impact().referenceUnit;
				default -> "";
			};
		}
	}

}
