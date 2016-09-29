package org.openlca.app.results;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
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
import org.openlca.core.database.EntityCache;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionResultProvider;

import com.google.common.base.Strings;

public class TotalImpactResultPage extends FormPage {

	private final static String COLUMN_NAME = M.Name;
	private final static String COLUMN_LOCATION = M.Location;
	private final static String COLUMN_CATEGORY = M.FlowCategory;
	private final static String COLUMN_AMOUNT = M.InventoryResult;
	private final static String COLUMN_FACTOR = M.ImpactFactor;
	private final static String COLUMN_RESULT = M.ImpactResult;
	private final static String[] COLUMN_LABELS = { COLUMN_NAME, COLUMN_LOCATION, COLUMN_CATEGORY, COLUMN_AMOUNT,
			COLUMN_FACTOR, COLUMN_RESULT };

	private final ContributionResultProvider<?> result;
	private final DQResult dqResult;
	private final ImpactFactorProvider impactFactors;

	private FormToolkit toolkit;
	private TreeViewer viewer;

	private boolean subgroupByProcesses = true;

	public TotalImpactResultPage(FormEditor editor, ContributionResultProvider<?> result, DQResult dqResult,
			ImpactFactorProvider impactFactors) {
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
		createImpactContributionTree(client);
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

	private void createImpactContributionTree(Composite parent) {
		String[] properties = COLUMN_LABELS;
		if (DQUI.displayExchangeQuality(dqResult)) {
			properties = DQUI.appendTableHeaders(properties, dqResult.setup.exchangeDqSystem);
		}
		LabelProvider labelProvider = new LabelProvider();
		viewer = Trees.createViewer(parent, properties, labelProvider);
		viewer.setContentProvider(new ContentProvider());
		toolkit.adapt(viewer.getTree(), false, false);
		toolkit.paintBordersFor(viewer.getTree());
		Actions.bind(viewer, TreeClipboard.onCopy(viewer));
		createColumnSorters(labelProvider);
		double[] widths = { .35, .15, .2, .10, .10, .10 };
		if (DQUI.displayExchangeQuality(dqResult)) {
			widths = DQUI.adjustTableWidths(widths, dqResult.setup.exchangeDqSystem);
		}
		Trees.bindColumnWidths(viewer.getTree(), widths);
		setInput();
	}

	private void createColumnSorters(LabelProvider p) {
		Viewers.sortByLabels(viewer, p, 0, 1, 2, 3, 4, 5);
		if (DQUI.displayExchangeQuality(dqResult)) {
			for (int i = 0; i < dqResult.setup.exchangeDqSystem.indicators.size(); i++) {
				Viewers.sortByDouble(viewer, p, i + 6);
			}
		}
	}

	private class LabelProvider extends DQLabelProvider {

		LabelProvider() {
			super(dqResult, dqResult != null ? dqResult.setup.exchangeDqSystem : null, 6);
		}

		@Override
		public Image getImage(Object element, int columnIndex) {
			if (columnIndex > 0)
				return null;
			if (!(element instanceof Item))
				return null;
			Item triple = (Item) element;
			return Images.get(triple.getType());
		}

		@Override
		public String getText(Object obj, int col) {
			if (!(obj instanceof Item))
				return null;
			Item item = (Item) obj;
			switch (item.getType()) {
			case IMPACT_CATEGORY:
				return getImpactText(item, col);
			case PROCESS:
				return getProcessText(item, col);
			case FLOW:
				return getFlowText(item, col);
			default:
				return null;
			}
		}

		private String getImpactText(Item item, int col) {
			ImpactCategoryDescriptor impact = item.impact;
			switch (col) {
			case 0:
				return impact.getName();
			case 5:
				return item.getResultString();
			default:
				return null;
			}
		}

		private String getProcessText(Item item, int col) {
			ProcessDescriptor process = item.process;
			switch (col) {
			case 0:
				return process.getName();
			case 1:
				EntityCache cache = result.cache;
				if (process.getLocation() == null)
					return null;
				Location location = cache.get(Location.class, process.getLocation());
				return location.getName();
			case 5:
				return item.getResultString();
			default:
				return null;
			}
		}

		private String getFlowText(Item item, int col) {
			FlowDescriptor flow = item.flow;
			ImpactCategoryDescriptor impact = item.impact;
			switch (col) {
			case 0:
				return flow.getName();
			case 2:
				return toString(Labels.getCategory(flow, result.cache));
			case 3:
				String value1 = Numbers.format(item.getFlowAmount());
				String unit1 = getReferenceUnit(flow);
				return value1 + " " + unit1;
			case 4:
				String value2 = Numbers.format(item.getImpactFactor());
				String unit2 = impact.getReferenceUnit() + "/" + getReferenceUnit(flow);
				return value2 + " " + unit2;
			case 5:
				return item.getResultString();
			default:
				return null;
			}
		}

		private String getReferenceUnit(FlowDescriptor flow) {
			FlowProperty property = result.cache.get(FlowProperty.class, flow.getRefFlowPropertyId());
			return property.getUnitGroup().getReferenceUnit().getName();
		}

		private String toString(Pair<String, String> pair) {
			if (Strings.isNullOrEmpty(pair.getLeft()) && Strings.isNullOrEmpty(pair.getRight()))
				return "";
			if (Strings.isNullOrEmpty(pair.getLeft()))
				return pair.getRight();
			if (Strings.isNullOrEmpty(pair.getRight()))
				return pair.getLeft();
			return pair.getLeft() + "/" + pair.getRight();
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
					if (child.getResult() != 0)
						children.add(child);
				}
			} else {
				for (FlowDescriptor flow : result.getFlowDescriptors()) {
					// process will be null in case of subgroupByProcesses=true
					Item child = new Item(parent.impact, parent.process, flow);
					if (child.getResult() != 0)
						children.add(child);
				}
			}
			children.sort((i1, i2) -> -Double.compare(i1.getResult(), i2.getResult()));
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

		double getFlowAmount() {
			if (process == null)
				return result.getTotalFlowResult(flow).value;
			return result.getSingleFlowResult(process, flow).value;
		}

		double getResult() {
			switch (getType()) {
			case IMPACT_CATEGORY:
				return result.getTotalImpactResult(impact).value;
			case PROCESS:
				return result.getSingleImpactResult(process, impact).value;
			case FLOW:
				return getImpactFactor() * getFlowAmount();
			default:
				return 0;
			}
		}

		String getResultString() {
			String s = Numbers.format(getResult());
			if (impact.getReferenceUnit() != null)
				s += " " + impact.getReferenceUnit();
			return s;
		}
	}
}
