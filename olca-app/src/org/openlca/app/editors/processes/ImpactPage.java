package org.openlca.app.editors.processes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.trees.Trees;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactCategoryDao;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.math.ReferenceAmount;
import org.openlca.core.matrix.DIndex;
import org.openlca.core.matrix.FlowIndex;
import org.openlca.core.matrix.ImpactBuilder;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.matrix.ParameterTable;
import org.openlca.core.matrix.format.MatrixBuilder;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.LocationDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ContributionResult;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.util.Strings;

class ImpactPage extends ModelPage<Process> {

	private Button zeroCheck;
	private TreeViewer tree;
	private ContributionResult result;

	ImpactPage(ProcessEditor editor) {
		super(editor, "ProcessImpactPage", M.ImpactAnalysis);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(this);
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		Composite comp = tk.createComposite(body);
		UI.gridLayout(comp, 3);
		UI.formLabel(comp, tk, M.ImpactAssessmentMethod);
		ImpactMethodViewer combo = new ImpactMethodViewer(comp);
		List<ImpactMethodDescriptor> list = new ImpactMethodDao(Database.get())
				.getDescriptors()
				.stream().sorted((m1, m2) -> Strings.compare(
						m1.name, m2.name))
				.collect(Collectors.toList());
		combo.setInput(list);
		combo.addSelectionChangedListener(m -> setTreeInput(m));

		zeroCheck = tk.createButton(comp, M.ExcludeZeroValues, SWT.CHECK);
		zeroCheck.setSelection(true);
		Controls.onSelect(
				zeroCheck, e -> setTreeInput(combo.getSelected()));

		tree = Trees.createViewer(body,
				M.Name, M.Category, M.Amount, M.Result);
		UI.gridData(tree.getControl(), true, true);
		tree.setContentProvider(new Content());
		tree.setLabelProvider(new Label());
		Trees.bindColumnWidths(tree.getTree(),
				0.25, 0.25, 0.25, 0.25);
		tree.getTree().getColumns()[2].setAlignment(SWT.RIGHT);
		tree.getTree().getColumns()[3].setAlignment(SWT.RIGHT);

		Action onOpen = Actions.onOpen(() -> {
			Contribution<?> c = Viewers.getFirstSelected(tree);
			if (c == null)
				return;
			if (c.item instanceof IndexFlow) {
				App.openEditor(((IndexFlow) c.item).flow);
			}
			if (c.item instanceof ImpactCategoryDescriptor) {
				App.openEditor((ImpactCategoryDescriptor) c.item);
			}
		});
		Actions.bind(tree, onOpen);
		Trees.onDoubleClick(tree, e -> onOpen.run());

		if (!list.isEmpty()) {
			ImpactMethodDescriptor m = list.get(0);
			combo.select(m);
			setTreeInput(m);
		}
		form.reflow(true);
	}

	private void setTreeInput(ImpactMethodDescriptor method) {
		if (result == null) {
			result = App.exec("Compute LCIA results ...", this::compute);
		}
		if (result == null
				|| !result.hasFlowResults()
				|| !result.hasImpactResults()) {
			tree.setInput(Collections.emptyList());
			return;
		}
		List<Contribution<?>> cons = new ImpactMethodDao(Database.get())
				.getCategoryDescriptors(method.id)
				.stream()
				.sorted((d1, d2) -> Strings.compare(d1.name, d2.name))
				.map(d -> Contribution.of(d, result.getTotalImpactResult(d)))
				.collect(Collectors.toList());
		tree.setInput(cons);
	}

	private ContributionResult compute() {
		IDatabase db = Database.get();
		ContributionResult r = new ContributionResult();

		// build the impact index
		r.impactIndex = new DIndex<>();
		new ImpactCategoryDao(db)
				.getDescriptors()
				.forEach(d -> r.impactIndex.put(d));

		// collect the elementary flow exchanges
		List<Exchange> elemFlows = new ArrayList<>();
		boolean regionalized = false;
		for (Exchange e : getModel().exchanges) {
			if (e.flow == null
					|| e.flow.flowType != FlowType.ELEMENTARY_FLOW)
				continue;
			if (e.location != null) {
				regionalized = true;
			}
			elemFlows.add(e);
		}

		// create the flow index and B matrix / vector
		r.flowIndex = regionalized
				? FlowIndex.createRegionalized()
				: FlowIndex.create();
		if (elemFlows.isEmpty())
			return r;
		MatrixBuilder enviBuilder = new MatrixBuilder();
		for (Exchange e : elemFlows) {
			FlowDescriptor flow = Descriptors.toDescriptor(e.flow);
			LocationDescriptor loc = e.location != null
					? Descriptors.toDescriptor(e.location)
					: null;
			int i = e.isInput
					? r.flowIndex.putInput(flow, loc)
					: r.flowIndex.putOutput(flow, loc);
			double amount = ReferenceAmount.get(e);
			if (e.isInput && amount != 0) {
				amount = -amount;
			}
			enviBuilder.add(i, 1, amount);
		}
		r.directFlowResults = enviBuilder.finish();
		r.totalFlowResults = r.directFlowResults.getColumn(0);

		// build the formula interpreter
		Set<Long> contexts = new HashSet<>();
		contexts.add(getModel().id);
		r.impactIndex.each((i, d) -> contexts.add(d.id));
		FormulaInterpreter interpreter = ParameterTable.interpreter(
				db, contexts, Collections.emptySet());

		// create the impact matrix and results
		r.impactFactors = new ImpactBuilder(db)
				.build(r.flowIndex, r.impactIndex, interpreter).impactMatrix;
		r.totalImpactResults = App.getSolver()
				.multiply(r.impactFactors, r.totalFlowResults);
		r.directFlowImpacts = r.impactFactors.copy();
		App.getSolver().scaleColumns(
				r.directFlowImpacts, r.totalFlowResults);

		return r;
	}

	private class Content extends ArrayContentProvider
			implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object obj) {
			if (!(obj instanceof Contribution))
				return null;
			Contribution<?> c = (Contribution<?>) obj;
			if (c.childs != null)
				return c.childs.toArray();
			if (!(c.item instanceof ImpactCategoryDescriptor))
				return null;

			ImpactCategoryDescriptor impact = (ImpactCategoryDescriptor) c.item;
			double total = result.getTotalImpactResult(impact);
			boolean withoutZeros = zeroCheck.getSelection();
			List<Contribution<?>> childs = new ArrayList<>();
			for (IndexFlow flow : result.getFlows()) {
				double value = result.getDirectFlowImpact(flow, impact);
				if (value == 0 && withoutZeros)
					continue;
				Contribution<?> child = Contribution.of(flow, value);
				child.computeShare(total);
				childs.add(child);
			}

			Collections.sort(childs,
					(c1, c2) -> Double.compare(c2.amount, c1.amount));
			c.childs = childs;
			return childs.toArray();

		}

		@Override
		public Object getParent(Object elem) {
			return null;
		}

		@Override
		public boolean hasChildren(Object elem) {
			if (!(elem instanceof Contribution))
				return false;
			Contribution<?> c = (Contribution<?>) elem;
			if (c.childs != null)
				return true;
			return c.item instanceof ImpactCategoryDescriptor;
		}
	}

	private class Label extends ColumnLabelProvider
			implements ITableLabelProvider {

		private ContributionImage img = new ContributionImage();

		@Override
		public void dispose() {
			img.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Contribution))
				return null;
			Contribution<?> c = (Contribution<?>) obj;
			if (col == 0) {
				return c.item instanceof ImpactCategoryDescriptor
						? Images.get(ModelType.IMPACT_CATEGORY)
						: Images.get(FlowType.ELEMENTARY_FLOW);
			}
			if (col == 3 && c.item instanceof IndexFlow)
				return img.getForTable(c.share);
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Contribution))
				return null;
			Contribution<?> c = (Contribution<?>) obj;
			switch (col) {
			case 0:
				if (c.item instanceof IndexFlow)
					return Labels.name((IndexFlow) c.item);
				if (c.item instanceof ImpactCategoryDescriptor) {
					ImpactCategoryDescriptor d = (ImpactCategoryDescriptor) c.item;
					return Strings.nullOrEmpty(d.referenceUnit)
							? Labels.name(d)
							: Labels.name(d) + " [" + d.referenceUnit + "]";
				}
				return null;
			case 1:
				if (c.item instanceof IndexFlow)
					return Labels.category((IndexFlow) c.item);
				return null;
			case 2:
				if (!(c.item instanceof IndexFlow))
					return null;
				IndexFlow iFlow = (IndexFlow) c.item;
				double a = result.getTotalFlowResult(iFlow);
				return Numbers.format(a) + " " + Labels.refUnit(iFlow);
			case 3:
				return Numbers.format(c.amount);
			default:
				return null;
			}
		}
	}
}
