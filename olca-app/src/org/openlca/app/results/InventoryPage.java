package org.openlca.app.results;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jface.action.Action;
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
import org.openlca.core.model.descriptors.CategorizedDescriptor;
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
		List<EnviFlow> inFlows = new ArrayList<>();
		List<EnviFlow> outFlows = new ArrayList<>();
		result.getFlows().forEach(f -> {
			if (f.isInput()) {
				inFlows.add(f);
			} else {
				outFlows.add(f);
			}
		});
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
		Label label = new Label();
		TreeViewer viewer = Trees.createViewer(comp, headers, label);
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
		Action onOpen = Actions.onOpen(() -> {
			Object obj = Viewers.getFirstSelected(viewer);
			if (obj instanceof CategorizedDescriptor) {
				App.open((CategorizedDescriptor) obj);
			}
			if (obj instanceof FlowContribution) {
				App.open(((FlowContribution) obj).item.item);
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
			if (!(e instanceof EnviFlow))
				return null;
			var flow = (EnviFlow) e;
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
			if (e instanceof FlowContribution)
				return ((FlowContribution) e).flow;
			return null;
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
			if (col == 0 && obj instanceof EnviFlow)
				return Images.get(((EnviFlow) obj).flow());
			if (!(obj instanceof FlowContribution))
				return null;
			FlowContribution c = (FlowContribution) obj;
			if (col == 0)
				return Images.get(c.item.item);
			if (col == 3)
				return img.get(c.item.share);
			return null;
		}

		@Override
		public String getText(Object obj, int col) {
			if (obj instanceof EnviFlow)
				return getFlowColumnText((EnviFlow) obj, col);
			if (obj instanceof FlowContribution)
				return getProcessColumnText((FlowContribution) obj, col);
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
			CategorizedDescriptor process = item.item.item;
			Pair<String, String> category = Labels.getCategory(process);
			switch (col) {
				case 0:
					return Labels.name(process);
				case 1:
					return category.getLeft();
				case 2:
					return category.getRight();
				case 3:
					double v = getAmount(item);
					return Numbers.format(v);
				case 4:
					return Labels.refUnit(item.flow);
				default:
					return null;
			}
		}

		@Override
		protected int[] getQuality(Object obj) {
			if (obj instanceof EnviFlow) {
				var f = (EnviFlow) obj;
				return dqResult.get(f);
			}
			if (obj instanceof FlowContribution) {
				FlowContribution item = (FlowContribution) obj;
				return dqResult.get(item.item.item, item.flow);
			}
			return null;
		}
	}

	private double getAmount(Object o) {
		if (o instanceof EnviFlow) {
			return result.getTotalFlowResult((EnviFlow) o);
		} else if (o instanceof FlowContribution) {
			FlowContribution item = (FlowContribution) o;
			return item.item.amount;
		}
		return 0d;
	}

	private static class FlowContribution {

		final Contribution<CategorizedDescriptor> item;
		final EnviFlow flow;

		private FlowContribution(
			Contribution<CategorizedDescriptor> item,
			EnviFlow flow) {
			this.item = item;
			this.flow = flow;
		}
	}

}
