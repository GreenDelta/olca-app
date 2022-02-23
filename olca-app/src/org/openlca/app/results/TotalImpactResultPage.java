package org.openlca.app.results;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.ContributionCutoff.CutoffContentProvider;
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
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.core.results.ContributionResult;

public class TotalImpactResultPage extends FormPage {

	private final CalculationSetup setup;
	private final ContributionResult result;
	private final DQResult dqResult;

	private FormToolkit toolkit;
	private TreeViewer viewer;
	private ContributionCutoff spinner;

	private boolean subgroupByProcesses = true;

	public TotalImpactResultPage(ResultEditor<?> editor) {
		super(editor, "ImpactTreePage", M.ImpactAnalysis);
		this.result = editor.result;
		this.setup = editor.setup;
		this.dqResult = editor.dqResult;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform,
			Labels.name(setup.target()),
			Images.get(result));
		toolkit = mform.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Section section = UI.section(body, toolkit, M.ImpactAnalysis + ": "
			+ Labels.name(setup.impactMethod()));
		UI.gridData(section, true, true);
		Composite client = toolkit.createComposite(section);
		section.setClient(client);
		UI.gridLayout(client, 1);
		createOptions(client);
		createTree(client);
		spinner.register(viewer);
		form.reflow(true);
	}

	private void createOptions(Composite parent) {
		Composite comp = UI.formComposite(parent, toolkit);
		UI.gridLayout(comp, 3);
		Button button = UI.formCheckBox(
			comp, toolkit, M.SubgroupByProcesses);
		button.setSelection(true);
		Controls.onSelect(button, (e) -> {
			subgroupByProcesses = button.getSelection();
			setInput();
		});
		spinner = ContributionCutoff.create(comp, toolkit);
	}

	private void setInput() {
		List<Item> items = result.getImpacts().stream()
			.map(Item::new)
			.collect(Collectors.toList());
		viewer.setInput(items);
	}

	private void createTree(Composite comp) {
		String[] columns = {M.Name, M.Category, M.InventoryResult,
			M.ImpactFactor, M.ImpactResult, M.Unit};
		if (DQUI.displayExchangeQuality(dqResult)) {
			columns = DQUI.appendTableHeaders(columns,
				dqResult.setup.exchangeSystem);
		}
		LabelProvider label = new LabelProvider();
		viewer = Trees.createViewer(comp, columns, label);
		viewer.setContentProvider(new ContentProvider());
		toolkit.adapt(viewer.getTree(), false, false);
		toolkit.paintBordersFor(viewer.getTree());

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
		Item item = Viewers.getFirstSelected(viewer);
		if (item == null)
			return;
		if (item.flow != null) {
			App.open(item.flow.flow());
		} else if (item.process != null) {
			App.open(item.process);
		} else if (item.impact != null) {
			App.open(item.impact);
		}
	}

	private void createColumnSorters(LabelProvider p) {
		Viewers.sortByLabels(viewer, p, 0, 1, 5);
		Viewers.sortByDouble(viewer, (item) -> ((Item) item).flowAmount(), 2);
		Viewers.sortByDouble(viewer, (item) -> ((Item) item).impactFactor(), 3);
		Viewers.sortByDouble(viewer, (item) -> ((Item) item).result(), 4);
		if (!DQUI.displayExchangeQuality(dqResult))
			return;
		for (int i = 0; i < dqResult.setup.exchangeSystem.indicators.size(); i++) {
			Viewers.sortByDouble(viewer, p, i + 5);
		}
	}

	private class ClipboardLabel implements ClipboardLabelProvider {

		private final LabelProvider label = new LabelProvider();

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
			Item item = (Item) treeItem.getData();
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
					return Labels.refUnit(item.flow);
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

	private class LabelProvider extends DQLabelProvider {

		private final ContributionImage img = new ContributionImage();

		LabelProvider() {
			super(dqResult, dqResult != null
				? dqResult.setup.exchangeSystem
				: null, 6);
		}

		@Override
		public void dispose() {
			img.dispose();
			super.dispose();
		}

		@Override
		public Image getImage(Object obj, int col) {
			if (!(obj instanceof Item item))
				return null;
			if (col == 0) {
				if (item.flow != null)
					return Images.get(item.flow);
				if (item.process != null)
					return Images.get(item.process);
				return Images.get(item.impact);
			}
			if (col == 4 && item.type() != ModelType.IMPACT_CATEGORY)
				return img.get(item.contribution());
			return null;
		}

		@Override
		public String getText(Object obj, int col) {
			if (!(obj instanceof Item item))
				return null;
			return switch (col) {
				case 0 -> item.name();
				case 1 -> item.category();
				case 2 -> item.flowAmountString();
				case 3 -> item.impactFactorString();
				case 4 -> Numbers.format(item.result());
				case 5 -> item.unit();
				default -> null;
			};
		}

		@Override
		protected int[] getQuality(Object obj) {
			if (dqResult == null)
				return null;
			if (!(obj instanceof Item item))
				return null;
			return switch (item.type()) {
				case IMPACT_CATEGORY -> dqResult.get(item.impact);
				case PROCESS -> dqResult.get(item.impact, item.process);
				case FLOW -> {
					if (item.flow == null)
						yield null;
					if (item.process != null)
						yield dqResult.get(item.process, item.flow);
					else
						yield dqResult.get(item.impact, item.flow);
				}
				default -> null;
			};
		}
	}

	private class ContentProvider extends ArrayContentProvider
		implements ITreeContentProvider, CutoffContentProvider {

		private double cutoff;

		@Override
		public Object[] getChildren(Object obj) {
			if (!(obj instanceof Item parent))
				return null;
			List<Item> childs = new ArrayList<>();

			double cutoffValue = Math.abs(parent.result() * cutoff);
			if (parent.type() == ModelType.IMPACT_CATEGORY && subgroupByProcesses) {
				for (var process : result.getProcesses()) {
					Item child = new Item(parent.impact, process);
					double result = child.result();
					if (result == 0)
						continue;
					if (Math.abs(result) >= cutoffValue) {
						childs.add(child);
					}
				}

			} else if (result.hasEnviFlows()) {
				result.enviIndex().each((i, f) -> {
					Item child = new Item(parent.impact, parent.process, f);
					double result = child.result();
					if (result == 0)
						return;
					if (Math.abs(result) >= cutoffValue) {
						childs.add(child);
					}
				});
			}

			childs.sort((i1, i2) -> -Double.compare(i1.result(), i2.result()));
			return childs.toArray();
		}

		@Override
		public Object getParent(Object o) {
			return null;
		}

		@Override
		public boolean hasChildren(Object o) {
			if (!(o instanceof Item item))
				return false;
			return item.type() != ModelType.FLOW;
		}

		@Override
		public void setCutoff(double cutoff) {
			this.cutoff = cutoff;
		}

	}

	private class Item {

		final ImpactDescriptor impact;
		final RootDescriptor process;
		final EnviFlow flow;

		Item(ImpactDescriptor impact) {
			this(impact, null, null);

		}

		Item(ImpactDescriptor impact, RootDescriptor process) {
			this(impact, process, null);
		}

		Item(ImpactDescriptor impact, RootDescriptor process,
			EnviFlow flow) {
			this.impact = impact;
			this.process = process;
			this.flow = flow;
		}

		/**
		 * The type of contribution shown by the item.
		 */
		ModelType type() {
			if (flow != null)
				return ModelType.FLOW;
			if (process != null)
				return ModelType.PROCESS;
			return ModelType.IMPACT_CATEGORY;
		}

		Double impactFactor() {
			if (flow == null)
				return null;
			return result.getImpactFactor(impact, flow);
		}

		String impactFactorUnit() {
			String iUnit = impact.referenceUnit;
			if (iUnit == null) {
				iUnit = "1";
			}
			String fUnit = flow != null
				? Labels.refUnit(flow)
				: "?";
			return iUnit + "/" + fUnit;
		}

		String impactFactorString() {
			if (type() != ModelType.FLOW)
				return null;
			var factor = impactFactor();
			if (factor == null)
				return null;
			return Numbers.format(factor) + " " + impactFactorUnit();
		}

		Double flowAmount() {
			if (flow == null)
				return null;
			if (process == null)
				return result.getTotalFlowResult(flow);
			return result.getDirectFlowResult(process, flow);
		}

		String flowAmountString() {
			if (type() != ModelType.FLOW)
				return null;
			var amount = flowAmount();
			if (amount == null)
				return null;
			String unit = Labels.refUnit(flow);
			return Numbers.format(amount) + " " + unit;
		}

		double result() {
			return switch (type()) {
				case IMPACT_CATEGORY -> result.getTotalImpactResult(impact);
				case PROCESS -> result.getDirectImpactResult(process, impact);
				case FLOW -> {
					var factor = impactFactor();
					var amount = flowAmount();
					yield factor == null || amount == null
						? 0
						: factor * amount;
				}
				default -> 0;
			};
		}

		String unit() {
			if (impact.referenceUnit == null)
				return null;
			return impact.referenceUnit;
		}

		String name() {
			return switch (type()) {
				case IMPACT_CATEGORY -> Labels.name(impact);
				case FLOW -> Labels.name(flow);
				case PROCESS -> Labels.name(process);
				default -> null;
			};
		}

		String category() {
			return switch (type()) {
				case IMPACT_CATEGORY -> Labels.category(impact);
				case FLOW -> Labels.category(flow);
				case PROCESS -> Labels.category(process);
				default -> null;
			};
		}

		double contribution() {
			double total = Math.abs(
				result.getTotalImpactResult(impact));
			double r = result();
			if (r == 0)
				return 0;
			if (total == 0)
				return r > 0 ? 1 : -1;
			return r / total;
		}
	}
}
