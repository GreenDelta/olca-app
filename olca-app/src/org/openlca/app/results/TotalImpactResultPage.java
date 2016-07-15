package org.openlca.app.results;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
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
import org.openlca.app.util.DQUIHelper;
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
import org.openlca.core.model.descriptors.BaseDescriptor;
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
		List<Triple> impacts = new ArrayList<>();
		for (ImpactCategoryDescriptor impact : result.getImpactDescriptors()) {
			impacts.add(new Triple(impact));
		}
		viewer.setInput(impacts);
	}

	private void createImpactContributionTree(Composite parent) {
		viewer = new TreeViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		LabelProvider labelProvider = new LabelProvider();
		viewer.setLabelProvider(labelProvider);
		viewer.setContentProvider(new ContentProvider());
		viewer.getTree().setLinesVisible(true);
		viewer.getTree().setHeaderVisible(true);
		String[] properties = COLUMN_LABELS;
		if (DQUIHelper.displayExchangeQuality(dqResult)) {
			properties = DQUIHelper.appendTableHeaders(properties, dqResult.exchangeSystem);
		}
		for (int i = 0; i < properties.length; i++) {
			TreeColumn c = new TreeColumn(viewer.getTree(), SWT.NULL);
			c.setText(properties[i]);
			c.pack();
		}
		viewer.setColumnProperties(properties);
		toolkit.adapt(viewer.getTree(), false, false);
		toolkit.paintBordersFor(viewer.getTree());
		UI.gridData(viewer.getTree(), true, true);
		Actions.bind(viewer, TreeClipboard.onCopy(viewer));
		createColumnSorters(labelProvider);
		double[] widths = { .35, .15, .2, .10, .10, .10 };
		if (DQUIHelper.displayExchangeQuality(dqResult)) {
			widths = DQUIHelper.adjustTableWidths(widths, dqResult.exchangeSystem);
		}
		Trees.bindColumnWidths(viewer.getTree(), widths);
		setInput();
	}

	private void createColumnSorters(LabelProvider p) {
		Viewers.sortByLabels(viewer, p, 0, 1, 2, 3, 4, 5);
		if (DQUIHelper.displayExchangeQuality(dqResult)) {
			for (int i = 0; i < dqResult.exchangeSystem.indicators.size(); i++) {
				Viewers.sortByDouble(viewer, p, i + 6);
			}
		}
	}

	private class LabelProvider extends DQLabelProvider {

		LabelProvider() {
			super(dqResult, 6);
		}

		@Override
		public Image getImage(Object element, int columnIndex) {
			if (columnIndex > 0)
				return null;
			if (!(element instanceof Triple))
				return null;
			Triple triple = (Triple) element;
			return Images.get(triple.getLast());
		}

		@Override
		public String getText(Object element, int columnIndex) {
			if (!(element instanceof Triple))
				return null;
			Triple triple = (Triple) element;
			BaseDescriptor elem = triple.getLast();
			if (elem instanceof ImpactCategoryDescriptor)
				return getImpactText(triple, columnIndex);
			if (elem instanceof ProcessDescriptor)
				return getProcessText(triple, columnIndex);
			if (elem instanceof FlowDescriptor)
				return getFlowText(triple, columnIndex);
			return null;
		}

		private String getImpactText(Triple triple, int columnIndex) {
			ImpactCategoryDescriptor impact = triple.impactCategory;
			switch (columnIndex) {
			case 0:
				return impact.getName();
			case 5:
				String value = Numbers.format(result.getTotalImpactResult(impact).value);
				String unit = impact.getReferenceUnit();
				return value + " " + unit;
			default:
				return null;
			}
		}

		private String getProcessText(Triple triple, int columnIndex) {
			ProcessDescriptor process = triple.process;
			ImpactCategoryDescriptor impact = triple.impactCategory;
			switch (columnIndex) {
			case 0:
				return process.getName();
			case 1:
				EntityCache cache = result.cache;
				if (process.getLocation() == null)
					return null;
				Location location = cache.get(Location.class, process.getLocation());
				return location.getName();
			case 5:
				String value = Numbers.format(result.getSingleImpactResult(process, impact).value);
				String unit = impact.getReferenceUnit();
				return value + " " + unit;
			default:
				return null;
			}
		}

		private String getFlowText(Triple triple, int columnIndex) {
			FlowDescriptor flow = triple.flow;
			ImpactCategoryDescriptor impact = triple.impactCategory;
			switch (columnIndex) {
			case 0:
				return flow.getName();
			case 2:
				return toString(Labels.getCategory(flow, result.cache));
			case 3:
				String value1 = Numbers.format(getFlowAmount(triple));
				String unit1 = getReferenceUnit(flow);
				return value1 + " " + unit1;
			case 4:
				String value2 = Numbers.format(getImpactFactor(triple));
				String unit2 = impact.getReferenceUnit() + "/" + getReferenceUnit(flow);
				return value2 + " " + unit2;
			case 5:
				String value3 = Numbers.format(getFlowResult(triple));
				String unit3 = impact.getReferenceUnit();
				return value3 + " " + unit3;
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
			Triple triple = (Triple) obj;
			BaseDescriptor elem = triple.getLast();
			if (elem instanceof ImpactCategoryDescriptor)
				return dqResult.get(triple.impactCategory);
			if (elem instanceof ProcessDescriptor)
				return dqResult.get(triple.process, triple.impactCategory);
			if (!(elem instanceof FlowDescriptor))
				return null;
			if (triple.process == null)
				return dqResult.get(triple.flow, triple.impactCategory);
			return dqResult.get(triple.process, triple.flow);
		}

	}

	private double getFlowAmount(Triple t) {
		if (t.process == null) {
			return result.getFlowContributions(t.impactCategory).getContribution(t.flow).amount;
		}
		return result.getSingleFlowResult(t.process, t.flow).value;
	}

	private double getImpactFactor(Triple t) {
		return impactFactors.get(t.impactCategory, t.process, t.flow);
	}

	private double getFlowResult(Triple t) {
		double factor = getImpactFactor(t);
		double amount = getFlowAmount(t);
		return factor * amount;
	}

	private class ContentProvider extends ArrayContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object parentElement) {
			if (!(parentElement instanceof Triple))
				return null;
			Triple triple = (Triple) parentElement;
			List<Triple> children = new ArrayList<>();
			if (triple.getLast() instanceof ImpactCategoryDescriptor && subgroupByProcesses) {
				for (ProcessDescriptor process : result.getProcessDescriptors()) {
					children.add(new Triple(triple.impactCategory, process));
				}
			} else {
				for (FlowDescriptor flow : result.getFlowDescriptors()) {
					// process will be null in case of subgroupByProcesses=true
					children.add(new Triple(triple.impactCategory, triple.process, flow));
				}
			}
			return children.toArray();
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (!(element instanceof Triple))
				return false;
			Triple triple = (Triple) element;
			if (triple.getLast() instanceof FlowDescriptor)
				return false;
			return true;
		}

	}

	public interface ImpactFactorProvider {

		double get(ImpactCategoryDescriptor impactCategory, ProcessDescriptor process, FlowDescriptor flow);

	}

	private class Triple {

		private final ImpactCategoryDescriptor impactCategory;
		private final ProcessDescriptor process;
		private final FlowDescriptor flow;

		private Triple(ImpactCategoryDescriptor impactCategory) {
			this(impactCategory, null, null);

		}

		private Triple(ImpactCategoryDescriptor impactCategory, ProcessDescriptor process) {
			this(impactCategory, process, null);
		}

		private Triple(ImpactCategoryDescriptor impactCategory, ProcessDescriptor process, FlowDescriptor flow) {
			this.impactCategory = impactCategory;
			this.process = process;
			this.flow = flow;
		}

		private BaseDescriptor getLast() {
			if (flow != null)
				return flow;
			if (process != null)
				return process;
			return impactCategory;
		}

	}
}
