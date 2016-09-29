package org.openlca.app.results;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.DQUI;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.trees.TreeClipboard;
import org.openlca.app.util.trees.Trees;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionResultProvider;

public class TotalImpactResultPage extends FormPage {

	private final ContributionResultProvider<?> result;
	private final DQResult dqResult;
	private final ImpactFactorProvider impactFactors;

	private FormToolkit toolkit;
	private TreeViewer viewer;

	private boolean subgroupByProcesses = true;

	public TotalImpactResultPage(FormEditor editor, ContributionResultProvider<?> result,
			DQResult dqResult, ImpactFactorProvider impactFactors) {
		super(editor, "ImpactTreePage", M.ImpactAnalysis);
		this.result = result;
		this.dqResult = dqResult;
		this.impactFactors = impactFactors;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, M.ImpactAnalysis);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Section section = UI.section(body, toolkit, M.ImpactAnalysis);
		UI.gridData(section, true, true);
		Composite client = toolkit.createComposite(section);
		section.setClient(client);
		UI.gridLayout(client, 1);
		createOptions(client);
		createTree(client);
		form.reflow(true);
	}

	private void createOptions(Composite parent) {
		Button button = UI.formCheckBox(parent, toolkit, "#Subgroup by processes");
		button.setSelection(true);
		Controls.onSelect(button, (e) -> {
			subgroupByProcesses = button.getSelection();
			setInput();
		});
	}

	private void setInput() {
		List<Item> impacts = new ArrayList<>();
		for (ImpactCategoryDescriptor impact : result.getImpactDescriptors()) {
			impacts.add(new Item(impact));
		}
		viewer.setInput(impacts);
	}

	private void createTree(Composite comp) {
		String[] columns = { M.Name, M.Category, M.InventoryResult,
				M.ImpactFactor, M.ImpactResult };
		if (DQUI.displayExchangeQuality(dqResult)) {
			columns = DQUI.appendTableHeaders(columns,
					dqResult.setup.exchangeDqSystem);
		}
		LabelProvider labelProvider = new LabelProvider();
		viewer = Trees.createViewer(comp, columns, labelProvider);
		viewer.setContentProvider(new ContentProvider());
		toolkit.adapt(viewer.getTree(), false, false);
		toolkit.paintBordersFor(viewer.getTree());
		Actions.bind(viewer, TreeClipboard.onCopy(viewer));
		createColumnSorters(labelProvider);
		double[] widths = { .35, .2, .10, .10, .20 };
		if (DQUI.displayExchangeQuality(dqResult)) {
			widths = DQUI.adjustTableWidths(widths, dqResult.setup.exchangeDqSystem);
		}
		Trees.bindColumnWidths(viewer.getTree(), widths);
		setInput();
	}

	private void createColumnSorters(LabelProvider p) {
		Viewers.sortByLabels(viewer, p, 0, 1, 2, 3, 4);
		// TODO: sort by values
		if (!DQUI.displayExchangeQuality(dqResult))
			return;
		for (int i = 0; i < dqResult.setup.exchangeDqSystem.indicators.size(); i++) {
			Viewers.sortByDouble(viewer, p, i + 5);
		}
	}

	private class LabelProvider extends DQLabelProvider {

		LabelProvider() {
			super(dqResult, dqResult != null ? dqResult.setup.exchangeDqSystem
					: null, 5);
		}

		@Override
		public Image getImage(Object obj, int col) {
			if (col > 0)
				return null;
			if (!(obj instanceof Item))
				return null;
			Item item = (Item) obj;
			return Images.get(item.getType());
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
				return item.resultString();
			default:
				return null;
			}
		}

		@Override
		protected double[] getQuality(Object obj) {
			if (dqResult == null)
				return null;
			Item item = (Item) obj;
			switch (item.getType()) {
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

	private class ContentProvider extends ArrayContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object obj) {
			if (!(obj instanceof Item))
				return null;
			Item parent = (Item) obj;
			List<Item> children = new ArrayList<>();
			if (parent.getType() == ModelType.IMPACT_CATEGORY && subgroupByProcesses) {
				for (ProcessDescriptor process : result.getProcessDescriptors()) {
					Item child = new Item(parent.impact, process);
					if (child.result() != 0)
						children.add(child);
				}
			} else {
				for (FlowDescriptor flow : result.getFlowDescriptors()) {
					// process will be null in case of subgroupByProcesses=true
					Item child = new Item(parent.impact, parent.process, flow);
					if (child.result() != 0)
						children.add(child);
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
			return item.getType() != ModelType.FLOW;
		}

	}

	public interface ImpactFactorProvider {

		double get(ImpactCategoryDescriptor impact, ProcessDescriptor process,
				FlowDescriptor flow);

	}

	private class Item {

		final ImpactCategoryDescriptor impact;
		final ProcessDescriptor process;
		final FlowDescriptor flow;

		Item(ImpactCategoryDescriptor impact) {
			this(impact, null, null);

		}

		Item(ImpactCategoryDescriptor impact, ProcessDescriptor process) {
			this(impact, process, null);
		}

		Item(ImpactCategoryDescriptor impact, ProcessDescriptor process,
				FlowDescriptor flow) {
			this.impact = impact;
			this.process = process;
			this.flow = flow;
		}

		/** The type of contribution shown by the item. */
		ModelType getType() {
			if (flow != null)
				return ModelType.FLOW;
			if (process != null)
				return ModelType.PROCESS;
			return ModelType.IMPACT_CATEGORY;
		}

		double getImpactFactor() {
			return impactFactors.get(impact, process, flow);
		}

		String impactFactorString() {
			if (getType() != ModelType.FLOW)
				return null;
			String f = Numbers.format(getImpactFactor());
			String unit = impact.getReferenceUnit();
			if (unit == null)
				unit = "1";
			unit += "/" + Labels.getRefUnit(flow, result.cache);
			return f + " " + unit;
		}

		double flowAmount() {
			if (process == null)
				return result.getTotalFlowResult(flow).value;
			return result.getSingleFlowResult(process, flow).value;
		}

		String flowAmountString() {
			if (getType() != ModelType.FLOW)
				return null;
			String amount = Numbers.format(flowAmount());
			String unit = Labels.getRefUnit(flow, result.cache);
			return amount + " " + unit;
		}

		double result() {
			switch (getType()) {
			case IMPACT_CATEGORY:
				return result.getTotalImpactResult(impact).value;
			case PROCESS:
				return result.getSingleImpactResult(process, impact).value;
			case FLOW:
				return getImpactFactor() * flowAmount();
			default:
				return 0;
			}
		}

		String name() {
			switch (getType()) {
			case IMPACT_CATEGORY:
				return impact.getName();
			case FLOW:
				return flow.getName();
			case PROCESS:
				if (process.getLocation() == null)
					return process.getName();
				else {
					String s = process.getName();
					Location loc = result.cache.get(Location.class,
							process.getLocation());
					if (loc != null && loc.getCode() != null) {
						s += " - " + loc.getCode();
					}
					return s;
				}
			default:
				return null;
			}
		}

		String category() {
			switch (getType()) {
			case FLOW:
				return Labels.getShortCategory(flow, result.cache);
			case PROCESS:
				return Labels.getShortCategory(process, result.cache);
			default:
				return null;
			}
		}

		String resultString() {
			String s = Numbers.format(result());
			if (impact.getReferenceUnit() != null)
				s += " " + impact.getReferenceUnit();
			return s;
		}
	}
}
