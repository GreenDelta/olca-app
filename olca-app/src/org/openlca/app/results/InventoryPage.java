package org.openlca.app.results;

import java.util.ArrayList;

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
import org.openlca.app.rcp.images.Icon;
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
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.core.results.Contribution;

/**
 * Shows the inventory result with process contributions.
 */
public class InventoryPage extends FormPage {

	private final ResultEditor editor;

	private FormToolkit toolkit;

	public InventoryPage(ResultEditor editor) {
		super(editor, "InventoryPage", M.InventoryResults);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.header(mform,
				Labels.name(editor.setup.target()),
				Icon.ANALYSIS_RESULT.get());
		toolkit = mform.getToolkit();
		var body = UI.body(form, toolkit);
		var sash = new SashForm(body, SWT.VERTICAL);
		UI.gridData(sash, true, true);
		toolkit.adapt(sash);
		var inputTree = createTree(sash, true);
		var outputTree = createTree(sash, false);
		var reqSection = new TotalRequirementsSection(
				editor.result, editor.dqResult);
		reqSection.create(sash, toolkit);
		form.reflow(true);
		fillTrees(inputTree, outputTree);
		reqSection.fill();
	}

	private void fillTrees(TreeViewer inputTree, TreeViewer outputTree) {
		var inFlows = new ArrayList<EnviFlow>();
		var outFlows = new ArrayList<EnviFlow>();
		for (var flow : editor.items.enviFlows()) {
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
		var headers = new String[]{
				M.Name, M.Category, M.Amount, M.Unit};
		if (DQUI.displayExchangeQuality(editor.dqResult)) {
			headers = DQUI.appendTableHeaders(
					headers, editor.dqResult.setup.exchangeSystem);
		}
		var label = new Label();
		var viewer = Trees.createViewer(comp, headers, label);
		viewer.setContentProvider(new ContentProvider());
		createColumnSorters(viewer, label);
		double[] widths = {.45, .35, .15, .05};
		if (DQUI.displayExchangeQuality(editor.dqResult)) {
			widths = DQUI.adjustTableWidths(
					widths, editor.dqResult.setup.exchangeSystem);
		}
		viewer.getTree().getColumns()[2].setAlignment(SWT.RIGHT);
		Trees.bindColumnWidths(viewer.getTree(), DQUI.MIN_COL_WIDTH, widths);

		// bind actions
		var onOpen = Actions.onOpen(() -> {
			var obj = Viewers.getFirstSelected(viewer);
			if (obj instanceof RootDescriptor d) {
				App.open(d);
			} else if (obj instanceof EnviFlow e) {
				App.open(e.flow());
			} else if (obj instanceof FlowContribution c) {
				App.open(c.item.item.provider());
			}
		});
		Trees.onDoubleClick(viewer, e -> onOpen.run());
		Actions.bind(viewer, onOpen, TreeClipboard.onCopy(viewer));
		spinner.register(viewer);
		return viewer;
	}

	private void createColumnSorters(TreeViewer viewer, Label label) {
		Viewers.sortByLabels(viewer, label, 0, 1, 3);
		Viewers.sortByDouble(viewer, this::getAmount, 2);
		if (DQUI.displayExchangeQuality(editor.dqResult)) {
			int len = editor.dqResult.setup.exchangeSystem.indicators.size();
			for (int i = 0; i < len; i++) {
				Viewers.sortByDouble(viewer, label, i + 4);
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
			return editor.result.getProcessContributions(flow)
					.stream()
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
			super(editor.dqResult, editor.dqResult != null
					? editor.dqResult.setup.exchangeSystem
					: null, 4);
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
				case 0 -> Images.get(c.item.item.provider());
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
			return switch (col) {
				case 0 -> Labels.name(f);
				case 1 -> Labels.category(f);
				case 2 -> Numbers.format(getAmount(f));
				case 3 -> Labels.refUnit(f);
				default -> null;
			};
		}

		private String getProcessColumnText(FlowContribution item, int col) {
			var techFlow = item.item.item;
			return switch (col) {
				case 0 -> Labels.name(techFlow.provider());
				case 1 -> Labels.category(techFlow);
				case 2 -> Numbers.format(getAmount(item));
				case 3 -> Labels.refUnit(item.flow);
				default -> null;
			};
		}

		@Override
		protected int[] getQuality(Object obj) {
			if (obj instanceof EnviFlow f)
				return editor.dqResult.get(f);
			if (obj instanceof FlowContribution c)
				return editor.dqResult.get(c.item.item, c.flow);
			return null;
		}
	}

	private double getAmount(Object o) {
		if (o instanceof EnviFlow e) {
			return editor.result.getTotalFlowValueOf(e);
		} else if (o instanceof FlowContribution c) {
			return c.item.amount;
		}
		return 0d;
	}

	private record FlowContribution(Contribution<TechFlow> item, EnviFlow flow) {
	}

}
