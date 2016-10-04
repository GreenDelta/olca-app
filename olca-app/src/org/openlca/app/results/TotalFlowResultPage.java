package org.openlca.app.results;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.DQUI;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.trees.TreeClipboard;
import org.openlca.app.util.trees.Trees;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.EntityCache;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.core.results.FlowResult;
import org.openlca.util.Strings;

/**
 * Shows the total inventory result of a quick calculation, analysis result,
 * etc.
 */
public class TotalFlowResultPage extends FormPage {

	private EntityCache cache = Cache.getEntityCache();
	private FormToolkit toolkit;
	private ContributionResultProvider<?> result;
	private DQResult dqResult;

	public TotalFlowResultPage(FormEditor editor, ContributionResultProvider<?> result, DQResult dqResult) {
		super(editor, "InventoryPage", M.InventoryResults);
		this.result = result;
		this.dqResult = dqResult;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, M.InventoryResults);
		toolkit = mform.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		TreeViewer inputTree = createSectionAndViewer(body, true);
		TreeViewer outputTree = createSectionAndViewer(body, false);
		TotalRequirementsSection reqSection = new TotalRequirementsSection(result, dqResult);
		reqSection.create(body, toolkit);
		form.reflow(true);
		fillTrees(inputTree, outputTree);
		reqSection.fill();
	}

	private void fillTrees(TreeViewer inputTree, TreeViewer outputTree) {
		Collection<FlowDescriptor> flows = result.getFlowDescriptors();
		List<FlowDescriptor> inFlows = new ArrayList<>();
		List<FlowDescriptor> outFlows = new ArrayList<>();
		for (FlowDescriptor flow : flows) {
			if (result.isInput(flow)) {
				inFlows.add(flow);
			} else {
				outFlows.add(flow);
			}
		}
		Comparator<FlowDescriptor> sorter = (f1, f2) -> Strings.compare(
				f1.getName(), f2.getName());
		Collections.sort(inFlows, sorter);
		Collections.sort(outFlows, sorter);
		inputTree.setInput(inFlows);
		outputTree.setInput(outFlows);
	}

	private TreeViewer createSectionAndViewer(Composite parent, boolean forInputs) {
		Section section = UI.section(parent, toolkit, forInputs ? M.Inputs : M.Outputs);
		UI.gridData(section, true, true);
		Composite comp = UI.sectionClient(section, toolkit);
		UI.gridLayout(comp, 1);
		String[] headers = new String[] { M.Name, M.Category, M.SubCategory, M.Amount };
		if (DQUI.displayExchangeQuality(dqResult)) {
			headers = DQUI.appendTableHeaders(headers, dqResult.setup.exchangeDqSystem);
		}
		Label label = new Label();
		TreeViewer viewer = Trees.createViewer(comp, headers, label);
		viewer.setContentProvider(new ContentProvider());
		createColumnSorters(viewer, label);
		double[] widths = { .4, .2, .2, .2 };
		if (DQUI.displayExchangeQuality(dqResult)) {
			widths = DQUI.adjustTableWidths(widths, dqResult.setup.exchangeDqSystem);
		}
		Trees.bindColumnWidths(viewer.getTree(), DQUI.MIN_COL_WIDTH, widths);
		Actions.bind(viewer, TreeClipboard.onCopy(viewer));
		return viewer;
	}

	private void createColumnSorters(TreeViewer viewer, Label label) {
		Viewers.sortByLabels(viewer, label, 0, 1, 2, 3);
		Function<FlowDescriptor, Double> amount = (f) -> {
			FlowResult r = result.getTotalFlowResult(f);
			return r == null ? 0 : r.value;
		};
		Viewers.sortByDouble(viewer, amount, 4);
		if (DQUI.displayExchangeQuality(dqResult)) {
			for (int i = 0; i < dqResult.setup.exchangeDqSystem.indicators.size(); i++) {
				Viewers.sortByDouble(viewer, label, i + 5);
			}
		}
	}

	private class ContentProvider extends ArrayContentProvider
			implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object e) {
			if (!(e instanceof FlowDescriptor))
				return null;
			FlowDescriptor flow = (FlowDescriptor) e;
			return result.getProcessContributions(flow).contributions.stream()
					.filter(i -> i.amount != 0)
					.sorted((i1, i2) -> -Double.compare(i1.amount, i2.amount))
					.map(i -> new Contribution(i, flow))
					.collect(Collectors.toList())
					.toArray();
		}

		@Override
		public Object getParent(Object e) {
			if (e instanceof Contribution)
				return ((Contribution) e).flow;
			return null;
		}

		@Override
		public boolean hasChildren(Object e) {
			return e instanceof FlowDescriptor;
		}
	}

	private class Label extends DQLabelProvider {

		private ContributionImage img = new ContributionImage(Display.getCurrent());

		Label() {
			super(dqResult, dqResult != null ? dqResult.setup.exchangeDqSystem : null, 4);
		}

		@Override
		public void dispose() {
			img.dispose();
			super.dispose();
		}

		@Override
		public Image getImage(Object obj, int col) {
			if (col == 0 && obj instanceof FlowDescriptor)
				return Images.get((FlowDescriptor) obj);
			if (!(obj instanceof Contribution))
				return null;
			Contribution c = (Contribution) obj;
			if (col == 0)
				return Images.get(c.item.item);
			if (col == 3)
				return img.getForTable(c.item.share);
			return null;
		}

		@Override
		public String getText(Object obj, int col) {
			if (obj instanceof FlowDescriptor)
				return getFlowColumnText((FlowDescriptor) obj, col);
			if (obj instanceof Contribution)
				return getProcessColumnText((Contribution) obj, col);
			return null;
		}

		private String getFlowColumnText(FlowDescriptor flow, int col) {
			Pair<String, String> category = Labels.getCategory(flow, cache);
			switch (col) {
			case 0:
				return Labels.getDisplayName(flow);
			case 1:
				return category.getLeft();
			case 2:
				return category.getRight();
			case 3:
				double v = result.getTotalFlowResult(flow).value;
				String unit = Labels.getRefUnit(flow, cache);
				return Numbers.format(v) + " " + unit;
			default:
				return null;
			}
		}

		private String getProcessColumnText(Contribution item, int col) {
			ProcessDescriptor process = item.item.item;
			Pair<String, String> category = Labels.getCategory(process, cache);
			switch (col) {
			case 0:
				return Labels.getDisplayName(process);
			case 1:
				return category.getLeft();
			case 2:
				return category.getRight();
			case 3:
				double v = item.item.amount;
				String unit = Labels.getRefUnit(item.flow, cache);
				return Numbers.format(v) + " " + unit;
			default:
				return null;
			}
		}

		@Override
		protected double[] getQuality(Object obj) {
			if (obj instanceof FlowDescriptor) {
				FlowDescriptor flow = (FlowDescriptor) obj;
				return dqResult.get(flow);
			}
			if (obj instanceof Contribution) {
				Contribution item = (Contribution) obj;
				return dqResult.get(item.item.item, item.flow);
			}
			return null;
		}

	}

	private class Contribution {

		final ContributionItem<ProcessDescriptor> item;
		final FlowDescriptor flow;

		private Contribution(ContributionItem<ProcessDescriptor> item, FlowDescriptor flow) {
			this.item = item;
			this.flow = flow;
		}
	}
}
