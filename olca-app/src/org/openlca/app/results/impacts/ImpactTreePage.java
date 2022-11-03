package org.openlca.app.results.impacts;

import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.ContributionCutoff;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.DQUI;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.trees.TreeClipboard;
import org.openlca.app.viewers.trees.TreeClipboard.ClipboardLabelProvider;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.results.LcaResult;
import org.openlca.core.results.ResultItemOrder;

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
		var form = UI.formHeader(mForm,
				Labels.name(setup.target()),
				Icon.ANALYSIS_RESULT.get());
		var tk = mForm.getToolkit();
		var body = UI.formBody(form, tk);
		var section = UI.section(body, tk,
				M.ImpactAnalysis + ": " + Labels.name(setup.impactMethod()));
		UI.gridData(section, true, true);
		var comp = tk.createComposite(section);
		section.setClient(comp);
		UI.gridLayout(comp, 1);
		createOptions(comp, tk);
		createTree(comp, tk);
		cutoff.register(viewer);
		form.reflow(true);
	}

	private void createOptions(Composite parent, FormToolkit tk) {
		var comp = UI.formComposite(parent, tk);
		UI.gridLayout(comp, 5);
		tk.createLabel(comp, "Sub-group by:");
		var flowCheck = tk.createButton(comp, M.Flows, SWT.RADIO);
		flowCheck.setSelection(flowsFirst);
		var processCheck = tk.createButton(comp, M.Processes, SWT.RADIO);
		processCheck.setSelection(!flowsFirst);
		Controls.onSelect(flowCheck, e -> {
			flowsFirst = flowCheck.getSelection();
			setInput();
		});
		tk.createLabel(comp, " | ");
		cutoff = ContributionCutoff.create(comp, tk);
	}

	private void setInput() {
		var items = editor.items.impacts()
				.stream()
				.map(impact -> ImpactItem.rootOf(result, impact))
				.collect(Collectors.toList());
		viewer.setInput(items);
	}

	private void createTree(Composite comp, FormToolkit tk) {
		String[] columns = {M.Name, M.Category, M.InventoryResult,
				M.ImpactFactor, M.ImpactResult, M.Unit};
		if (DQUI.displayExchangeQuality(dqResult)) {
			columns = DQUI.appendTableHeaders(columns,
					dqResult.setup.exchangeSystem);
		}
		var label = new TreeLabel(editor.dqResult);
		viewer = Trees.createViewer(comp, columns, label);
		viewer.setContentProvider(new TreeContent(this));
		tk.adapt(viewer.getTree(), false, false);
		tk.paintBordersFor(viewer.getTree());

		Action onOpen = Actions.onOpen(this::onOpen);
		Actions.bind(viewer, onOpen,
				TreeClipboard.onCopy(viewer, new ClipboardLabel()));
		Trees.onDoubleClick(viewer, e -> onOpen.run());
		createColumnSorters(label);
		double[] widths = {.35, .2, .10, .10, .15, .05};
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
		ImpactItem item = Viewers.getFirstSelected(viewer);
		if (item == null)
			return;
		if (item.enviFlow() != null) {
			App.open(item.enviFlow().flow());
		} else if (item.techFlow() != null) {
			App.open(item.techFlow().provider());
		} else if (item.impact() != null) {
			App.open(item.impact());
		}
	}

	private void createColumnSorters(TreeLabel p) {
		Viewers.sortByLabels(viewer, p, 0, 1, 5);
		Viewers.sortByDouble(viewer, (item) -> ((ImpactItem) item).flowAmount(), 2);
		Viewers.sortByDouble(viewer, (item) -> ((ImpactItem) item).impactFactor(), 3);
		Viewers.sortByDouble(viewer, (item) -> ((ImpactItem) item).amount(), 4);
		if (!DQUI.displayExchangeQuality(dqResult))
			return;
		for (int i = 0; i < dqResult.setup.exchangeSystem.indicators.size(); i++) {
			Viewers.sortByDouble(viewer, p, i + 5);
		}
	}

	private class ClipboardLabel implements ClipboardLabelProvider {

		private final TreeLabel label = new TreeLabel(editor.dqResult);

		private final String[] columns = {
				M.Name,
				M.Category,
				M.InventoryResult,
				M.Unit,
				M.ImpactFactor,
				M.Unit,
				M.ImpactResult,
				M.Unit
		};

		@Override
		public int columns() {
			return columns.length;
		}

		@Override
		public String getHeader(int col) {
			return columns[col];
		}

		@Override
		public String getLabel(TreeItem treeItem, int col) {
			ImpactItem item = (ImpactItem) treeItem.getData();
			switch (col) {
				case 0:
					return label.getText(item, 0);
				case 1:
					return label.getText(item, 1);
				case 2:
					return format(item.flowAmount());
				case 3:
					if (item.flowAmount() == null)
						return "";
					return Labels.refUnit(item.enviFlow());
				case 4:
					return format(item.impactFactor());
				case 5:
					if (item.impactFactor() == null)
						return "";
					return item.impactFactorUnit();
				case 6:
					return label.getText(item, 4);
				case 7:
					return label.getText(item, 5);
			}
			return null;
		}

		private String format(Double d) {
			if (d == null)
				return "";
			return Numbers.format(d);
		}
	}

}
