package org.openlca.app.results;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
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
import org.openlca.app.util.trees.TreeClipboard;
import org.openlca.app.util.trees.TreeClipboard.ClipboardLabelProvider;
import org.openlca.app.util.trees.Trees;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.ContributionResult;

public class TotalImpactResultPage extends FormPage {

	private final ContributionResult result;
	private final DQResult dqResult;
	private final ImpactFactorProvider impactFactors;

	private FormToolkit toolkit;
	private TreeViewer viewer;
	private ContributionCutoff spinner;
	private CalculationSetup setup;

	private boolean subgroupByProcesses = true;

	public TotalImpactResultPage(FormEditor editor, ContributionResult result,
			DQResult dqResult, CalculationSetup setup, ImpactFactorProvider impactFactors) {
		super(editor, "ImpactTreePage", M.ImpactAnalysis);
		this.result = result;
		this.setup = setup;
		this.dqResult = dqResult;
		this.impactFactors = impactFactors;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform,
				Labels.getDisplayName(setup.productSystem),
				Images.get(result));
		toolkit = mform.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Section section = UI.section(body, toolkit, M.ImpactAnalysis);
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
		Composite container = UI.formComposite(parent, toolkit);
		UI.gridLayout(container, 3);
		Button button = UI.formCheckBox(container, toolkit, M.SubgroupByProcesses);
		button.setSelection(true);
		Controls.onSelect(button, (e) -> {
			subgroupByProcesses = button.getSelection();
			setInput();
		});
		spinner = ContributionCutoff.create(container, toolkit);
	}

	private void setInput() {
		List<Item> impacts = new ArrayList<>();
		for (ImpactCategoryDescriptor impact : result.getImpacts()) {
			impacts.add(new Item(impact));
		}
		viewer.setInput(impacts);
	}

	private void createTree(Composite comp) {
		String[] columns = { M.Name, M.Category, M.InventoryResult,
				M.ImpactFactor, M.ImpactResult, M.Unit };
		if (DQUI.displayExchangeQuality(dqResult)) {
			columns = DQUI.appendTableHeaders(columns,
					dqResult.setup.exchangeDqSystem);
		}
		LabelProvider labelProvider = new LabelProvider();
		viewer = Trees.createViewer(comp, columns, labelProvider);
		viewer.setContentProvider(new ContentProvider());
		toolkit.adapt(viewer.getTree(), false, false);
		toolkit.paintBordersFor(viewer.getTree());

		Action onOpen = Actions.onOpen(this::onOpen);
		Actions.bind(viewer, onOpen,
				TreeClipboard.onCopy(viewer, new ClipboardLabel()));
		Trees.onDoubleClick(viewer, e -> onOpen.run());
		createColumnSorters(labelProvider);
		double[] widths = { .35, .2, .10, .10, .15, .05 };
		if (DQUI.displayExchangeQuality(dqResult)) {
			widths = DQUI.adjustTableWidths(widths, dqResult.setup.exchangeDqSystem);
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
		if (item.flow != null)
			App.openEditor(item.flow);
		else if (item.process != null)
			App.openEditor(item.process);
		else if (item.impact != null)
			App.openEditor(setup.impactMethod);
	}

	private void createColumnSorters(LabelProvider p) {
		Viewers.sortByLabels(viewer, p, 0, 1, 5);
		Viewers.sortByDouble(viewer, (item) -> ((Item) item).flowAmount(), 2);
		Viewers.sortByDouble(viewer, (item) -> ((Item) item).impactFactor(), 3);
		Viewers.sortByDouble(viewer, (item) -> ((Item) item).result(), 4);
		if (!DQUI.displayExchangeQuality(dqResult))
			return;
		for (int i = 0; i < dqResult.setup.exchangeDqSystem.indicators.size(); i++) {
			Viewers.sortByDouble(viewer, p, i + 5);
		}
	}

	private class ClipboardLabel implements ClipboardLabelProvider {

		private LabelProvider label = new LabelProvider();

		private String[] columns = {
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
				return item.flowAmountUnit();
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

		private ContributionImage img = new ContributionImage(Display.getCurrent());

		LabelProvider() {
			super(dqResult, dqResult != null ? dqResult.setup.exchangeDqSystem : null, 6);
		}

		@Override
		public void dispose() {
			img.dispose();
			super.dispose();
		}

		@Override
		public Image getImage(Object obj, int col) {
			if (!(obj instanceof Item))
				return null;
			Item item = (Item) obj;
			if (col == 0)
				return Images.get(item.type());
			if (col == 4 && item.type() != ModelType.IMPACT_CATEGORY)
				return img.getForTable(item.contribution());
			return null;
		}

		@Override
		public String getText(Object obj, int col) {
			if (!(obj instanceof Item))
				return null;
			Item item = (Item) obj;
			switch (col) {
			case 0:
				return item.name();
			case 1:
				return item.category();
			case 2:
				return item.flowAmountString();
			case 3:
				return item.impactFactorString();
			case 4:
				return Numbers.format(item.result());
			case 5:
				return item.unit();
			default:
				return null;
			}
		}

		@Override
		protected double[] getQuality(Object obj) {
			if (dqResult == null)
				return null;
			Item item = (Item) obj;
			switch (item.type()) {
			case IMPACT_CATEGORY:
				return dqResult.get(item.impact);
			case PROCESS:
				return dqResult.get(item.process, item.impact);
			case FLOW:
				if (item.process != null)
					return dqResult.get(item.process, item.flow);
				else
					return dqResult.get(item.flow, item.impact);
			default:
				return null;
			}
		}
	}

	private class ContentProvider extends ArrayContentProvider implements ITreeContentProvider, CutoffContentProvider {

		private double cutoff;

		@Override
		public Object[] getChildren(Object obj) {
			if (!(obj instanceof Item))
				return null;
			Item parent = (Item) obj;
			List<Item> children = new ArrayList<>();
			if (parent.type() == ModelType.IMPACT_CATEGORY && subgroupByProcesses) {
				double cutoffValue = Math.abs(parent.result() * cutoff);
				for (CategorizedDescriptor process : result.getProcesses()) {
					Item child = new Item(parent.impact, process);
					double result = child.result();
					if (result == 0)
						continue;
					if (Math.abs(result) >= cutoffValue) {
						children.add(child);
					}
				}
			} else {
				double cutoffValue = Math.abs(parent.result() * cutoff);
				for (FlowDescriptor flow : result.getFlows()) {
					// process will be null in case of subgroupByProcesses=false
					Item child = new Item(parent.impact, parent.process, flow);
					double result = child.result();
					if (result == 0)
						continue;
					if (Math.abs(result) >= cutoffValue) {
						children.add(child);
					}
				}
			}
			children.sort((i1, i2) -> -Double.compare(i1.result(), i2.result()));
			return children.toArray();
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (!(element instanceof Item))
				return false;
			Item item = (Item) element;
			if (item.type() == ModelType.FLOW)
				return false;
			return true;
		}

		@Override
		public void setCutoff(double cutoff) {
			this.cutoff = cutoff;
		}

	}

	public interface ImpactFactorProvider {

		double get(ImpactCategoryDescriptor impact,
				CategorizedDescriptor process,
				FlowDescriptor flow);

	}

	private class Item {

		final ImpactCategoryDescriptor impact;
		final CategorizedDescriptor process;
		final FlowDescriptor flow;

		Item(ImpactCategoryDescriptor impact) {
			this(impact, null, null);

		}

		Item(ImpactCategoryDescriptor impact, CategorizedDescriptor process) {
			this(impact, process, null);
		}

		Item(ImpactCategoryDescriptor impact, CategorizedDescriptor process,
				FlowDescriptor flow) {
			this.impact = impact;
			this.process = process;
			this.flow = flow;
		}

		/** The type of contribution shown by the item. */
		ModelType type() {
			if (flow != null)
				return ModelType.FLOW;
			if (process != null)
				return ModelType.PROCESS;
			return ModelType.IMPACT_CATEGORY;
		}

		Double impactFactor() {
			// note that process can be null if we want to get the
			// total flow contribution
			if (flow == null)
				return null;
			return impactFactors.get(impact, process, flow);
		}

		String impactFactorUnit() {
			String unit = impact.referenceUnit;
			if (unit == null)
				unit = "1";
			return unit + "/" + Labels.getRefUnit(flow);
		}

		String impactFactorString() {
			if (type() != ModelType.FLOW)
				return null;
			String f = Numbers.format(impactFactor());
			String unit = impactFactorUnit();
			return f + " " + unit;
		}

		Double flowAmount() {
			if (flow == null)
				return null;
			if (process == null)
				return result.getTotalFlowResult(flow);
			return result.getDirectFlowResult(process, flow);
		}

		String flowAmountUnit() {
			return Labels.getRefUnit(flow);
		}

		String flowAmountString() {
			if (type() != ModelType.FLOW)
				return null;
			String amount = Numbers.format(flowAmount());
			String unit = flowAmountUnit();
			return amount + " " + unit;
		}

		double result() {
			switch (type()) {
			case IMPACT_CATEGORY:
				return result.getTotalImpactResult(impact);
			case PROCESS:
				return result.getDirectImpactResult(process, impact);
			case FLOW:
				return impactFactor() * flowAmount();
			default:
				return 0;
			}
		}

		String unit() {
			if (impact.referenceUnit == null)
				return null;
			return impact.referenceUnit;
		}

		String name() {
			switch (type()) {
			case IMPACT_CATEGORY:
				return impact.name;
			case FLOW:
				return flow.name;
			case PROCESS:
				return Labels.getDisplayName(process);
			default:
				return null;
			}
		}

		String category() {
			switch (type()) {
			case FLOW:
				return Labels.getShortCategory(flow);
			case PROCESS:
				return Labels.getShortCategory(process);
			default:
				return null;
			}
		}

		double contribution() {
			double total = Math.abs(result.getTotalImpactResult(impact));
			double r = result();
			if (r == 0)
				return 0;
			if (total == 0)
				return r > 0 ? 1 : -1;
			return r / total;
		}
	}
}
