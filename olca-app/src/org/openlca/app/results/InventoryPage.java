package org.openlca.app.results;

import java.util.ArrayList;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.ContributionCutoff.CutoffContentProvider;
import org.openlca.app.results.requirements.TotalRequirementsSection;
import org.openlca.app.util.Actions;
import org.openlca.app.util.DQUI;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.trees.TreeClipboard;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ContributionResult;

/**
 * Shows the inventory result with process contributions.
 */
public class InventoryPage extends FormPage {

	private final CalculationSetup setup;
	private final ContributionResult result;
	private final DQResult dqResult;

	private FormToolkit toolkit;

	public InventoryPage(ResultEditor<?> editor) {
		super(editor, "InventoryPage", M.InventoryResults);
		this.result = editor.result;
		this.setup = editor.setup;
		this.dqResult = editor.dqResult;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(mform,
			Labels.name(setup.target()),
			Images.get(result));
		toolkit = mform.getToolkit();
		var body = UI.formBody(form, toolkit);
		var sash = new SashForm(body, SWT.VERTICAL);
		UI.gridData(sash, true, true);
		toolkit.adapt(sash);
		var inputTree = createTree(sash, true);
		var outputTree = createTree(sash, false);
		var reqSection = new TotalRequirementsSection(result, dqResult);
		reqSection.create(sash, toolkit);
		form.reflow(true);
		fillTrees(inputTree, outputTree);
		reqSection.fill();
	}

	private void fillTrees(TreeViewer inputTree, TreeViewer outputTree) {
		var inFlows = new ArrayList<EnviFlow>();
		var outFlows = new ArrayList<EnviFlow>();
		for (var flow : result.getFlows()) {
			if (flow.isVirtual())
				continue;
			var list = flow.isInput() ? inFlows : outFlows;
			list.add(flow);
		}
		inputTree.setInput(inFlows);
		outputTree.setInput(outFlows);
	}

	private TreeViewer createTree(Composite parent, boolean forInputs) {

		// create section and cutoff combo
		var section = UI.section(parent, toolkit,
			forInputs ? M.Inputs : M.Outputs);
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, toolkit, 1);
		var spinner = ContributionCutoff.create(comp, toolkit);

		// create the tree
		String[] headers = new String[]{
			M.Name, M.Category, M.SubCategory, M.Amount, M.Unit};
		if (DQUI.displayExchangeQuality(dqResult)) {
			headers = DQUI.appendTableHeaders(
				headers, dqResult.setup.exchangeSystem);
		}
		var label = new Label();
		var viewer = Trees.createViewer(comp, headers, label);
		viewer.setContentProvider(new ContentProvider());
		createColumnSorters(viewer, label);
		double[] widths = {.4, .2, .2, .15, .05};
		if (DQUI.displayExchangeQuality(dqResult)) {
			widths = DQUI.adjustTableWidths(
				widths, dqResult.setup.exchangeSystem);
		}
		viewer.getTree().getColumns()[3].setAlignment(SWT.RIGHT);
		Trees.bindColumnWidths(viewer.getTree(), DQUI.MIN_COL_WIDTH, widths);

		// bind actions
		var onOpen = Actions.onOpen(() -> {
			var obj = Viewers.getFirstSelected(viewer);
			if (obj instanceof RootDescriptor d) {
				App.open(d);
			}
			if (obj instanceof FlowContribution c) {
				App.open(c.item.item);
			}
		});
		Trees.onDoubleClick(viewer, e -> onOpen.run());
		Actions.bind(viewer, onOpen, TreeClipboard.onCopy(viewer));
		spinner.register(viewer);
		return viewer;
	}

	private void createColumnSorters(TreeViewer viewer, Label label) {
		Viewers.sortByLabels(viewer, label, 0, 1, 2, 4);
		Viewers.sortByDouble(viewer, this::getAmount, 3);
		if (DQUI.displayExchangeQuality(dqResult)) {
			int len = dqResult.setup.exchangeSystem.indicators.size();
			for (int i = 0; i < len; i++) {
				Viewers.sortByDouble(viewer, label, i + 5);
			}
		}
	}

	private class ContentProvider extends ArrayContentProvider
		implements ITreeContentProvider, CutoffContentProvider {

		private double cutoff;

		@Override
		public Object[] getChildren(Object e) {
			if (!(e instanceof EnviFlow flow))
				return null;
			double cutoffValue = Math.abs(getAmount(flow) * this.cutoff);
			return result.getProcessContributions(flow).stream()
				.filter(i -> i.amount != 0)
				.filter(i -> Math.abs(i.amount) >= cutoffValue)
				.sorted((i1, i2) -> -Double.compare(i1.amount, i2.amount))
				.map(i -> new FlowContribution(i, flow))
				.toArray();
		}

		@Override
		public Object getParent(Object e) {
			return e instanceof FlowContribution f
				? f.flow
				: null;
		}

		@Override
		public boolean hasChildren(Object e) {
			return e instanceof EnviFlow;
		}

		@Override
		public void setCutoff(double cutoff) {
			this.cutoff = cutoff;
		}
	}

	private class Label extends DQLabelProvider {

		private final ContributionImage img = new ContributionImage();

		Label() {
			super(dqResult, dqResult != null
				? dqResult.setup.exchangeSystem
				: null, 5);
		}

		@Override
		public void dispose() {
			img.dispose();
			super.dispose();
		}

		@Override
		public Image getImage(Object obj, int col) {
			if (col == 0 && obj instanceof EnviFlow e)
				return Images.get(e.flow());
			if (!(obj instanceof FlowContribution c))
				return null;
			return switch (col) {
				case 0 -> Images.get(c.item.item);
				case 3 -> img.get(c.item.share);
				default -> null;
			};
		}

		@Override
		public String getText(Object obj, int col) {
			if (obj instanceof EnviFlow e)
				return getFlowColumnText(e, col);
			if (obj instanceof FlowContribution c)
				return getProcessColumnText(c, col);
			return null;
		}

		private String getFlowColumnText(EnviFlow f, int col) {
			if (f.flow() == null)
				return null;
			Pair<String, String> category = Labels.getCategory(f.flow());
			return switch (col) {
				case 0 -> Labels.name(f);
				case 1 -> category.getLeft();
				case 2 -> category.getRight();
				case 3 -> Numbers.format(getAmount(f));
				case 4 -> Labels.refUnit(f);
				default -> null;
			};
		}

		private String getProcessColumnText(FlowContribution item, int col) {
			var process = item.item.item;
			var category = Labels.getCategory(process);
			return switch (col) {
				case 0 -> Labels.name(process);
				case 1 -> category.getLeft();
				case 2 -> category.getRight();
				case 3 -> Numbers.format(getAmount(item));
				case 4 -> Labels.refUnit(item.flow);
				default -> null;
			};
		}

		@Override
		protected int[] getQuality(Object obj) {
			if (obj instanceof EnviFlow f)
				return dqResult.get(f);
			if (obj instanceof FlowContribution c)
				return dqResult.get(c.item.item, c.flow);
			return null;
		}
	}

	private double getAmount(Object o) {
		if (o instanceof EnviFlow e) {
			return result.getTotalFlowResult(e);
		} else if (o instanceof FlowContribution c) {
			return c.item.amount;
		}
		return 0d;
	}

	private record FlowContribution(
		Contribution<RootDescriptor> item,
		EnviFlow flow) {

	}

}
