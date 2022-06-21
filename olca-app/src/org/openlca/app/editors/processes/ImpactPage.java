package org.openlca.app.editors.processes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.math.ReferenceAmount;
import org.openlca.core.matrix.Demand;
import org.openlca.core.matrix.ImpactBuilder;
import org.openlca.core.matrix.MatrixData;
import org.openlca.core.matrix.ParameterTable;
import org.openlca.core.matrix.format.JavaMatrix;
import org.openlca.core.matrix.format.MatrixBuilder;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.EnviIndex;
import org.openlca.core.matrix.index.ImpactIndex;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.matrix.index.TechIndex;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.providers.EagerResultProvider;
import org.openlca.core.results.providers.SolverContext;
import org.openlca.util.Strings;

class ImpactPage extends ModelPage<Process> {

	private ImpactMethodViewer combo;
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
		UI.gridLayout(comp, 4);
		UI.formLabel(comp, tk, M.ImpactAssessmentMethod);
		combo = new ImpactMethodViewer(comp);
		List<ImpactMethodDescriptor> list = new ImpactMethodDao(Database.get())
				.getDescriptors()
				.stream().sorted((m1, m2) -> Strings.compare(
						m1.name, m2.name))
				.collect(Collectors.toList());
		combo.setInput(list);
		combo.addSelectionChangedListener(this::setTreeInput);

		zeroCheck = tk.createButton(comp, M.ExcludeZeroValues, SWT.CHECK);
		zeroCheck.setSelection(true);
		Controls.onSelect(
				zeroCheck, e -> setTreeInput(combo.getSelected()));

		Button reload = tk.createButton(comp, M.Reload, SWT.NONE);
		reload.setImage(Icon.REFRESH.get());
		Controls.onSelect(reload, _e -> {
			result = null;
			setTreeInput(combo.getSelected());
		});

		tree = Trees.createViewer(body,
				M.Name, M.Category, M.Amount, M.Result);
		UI.gridData(tree.getControl(), true, true);
		tree.setContentProvider(new Content());
		tree.setLabelProvider(new Label());
		Trees.bindColumnWidths(tree.getTree(),
				0.35, 0.35, 0.15, 0.15);
		tree.getTree().getColumns()[2].setAlignment(SWT.RIGHT);
		tree.getTree().getColumns()[3].setAlignment(SWT.RIGHT);

		Action onOpen = Actions.onOpen(() -> {
			Contribution<?> c = Viewers.getFirstSelected(tree);
			if (c == null)
				return;
			if (c.item instanceof EnviFlow) {
				App.open(((EnviFlow) c.item).flow());
			}
			if (c.item instanceof ImpactDescriptor) {
				App.open((ImpactDescriptor) c.item);
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
		if (tree == null)
			return;
		if (method == null) {
			tree.setInput(Collections.emptyList());
			return;
		}
		if (result == null) {
			App.runInUI("Compute LCIA results ...", () -> {
				result = compute();
				setTreeInput(method);
			});
			return;
		}
		if (!result.hasEnviFlows() || !result.hasImpacts()) {
			tree.setInput(Collections.emptyList());
			return;
		}
		List<Contribution<?>> cons = new ImpactMethodDao(Database.get())
				.getCategoryDescriptors(method.id)
				.stream()
				.sorted((d1, d2) -> Strings.compare(d1.name, d2.name))
				.map(d -> {
					var c = Contribution.of(d, result.getTotalImpactResult(d));
					c.unit = d.referenceUnit;
					return c;
				})
				.collect(Collectors.toList());
		tree.setInput(cons);
	}

	private ContributionResult compute() {

		var data = new MatrixData();

		// create a virtual demand of 1.0
		var refProduct = TechFlow.of(getModel());
		data.techIndex = new TechIndex(refProduct);
		data.demand = Demand.of(refProduct, 1.0);
		data.techMatrix = JavaMatrix.of(
				new double[][] { { 1.0 } });

		// collect the elementary flow exchanges
		var elemFlows = new ArrayList<Exchange>();
		boolean regionalized = false;
		for (var e : getModel().exchanges) {
			if (e.flow == null
					|| e.flow.flowType != FlowType.ELEMENTARY_FLOW)
				continue;
			if (e.location != null) {
				regionalized = true;
			}
			elemFlows.add(e);
		}
		if (elemFlows.isEmpty()) {
			// return an empty result if there are no elementary flows
			var provider = EagerResultProvider.create(SolverContext.of(data));
			return new ContributionResult(provider);
		}

		// create the flow index and B matrix / vector
		data.enviIndex = regionalized
				? EnviIndex.createRegionalized()
				: EnviIndex.create();
		var enviBuilder = new MatrixBuilder();
		for (var e : elemFlows) {
			var flow = Descriptor.of(e.flow);
			var loc = e.location != null
					? Descriptor.of(e.location)
					: null;
			int i = e.isInput
					? data.enviIndex.add(EnviFlow.inputOf(flow, loc))
					: data.enviIndex.add(EnviFlow.outputOf(flow, loc));
			double amount = ReferenceAmount.get(e);
			if (e.isInput && amount != 0) {
				amount = -amount;
			}
			enviBuilder.add(i, 0, amount);
		}
		data.enviMatrix = enviBuilder.finish();

		// build the impact index and matrix
		var db = Database.get();
		data.impactIndex = ImpactIndex.of(db);
		var contexts = new HashSet<Long>();
		contexts.add(getModel().id);
		data.impactIndex.each((i, d) -> contexts.add(d.id));
		var interpreter = ParameterTable.interpreter(
				db, contexts, Collections.emptySet());
		data.impactMatrix = ImpactBuilder.of(db, data.enviIndex)
				.withImpacts(data.impactIndex)
				.withInterpreter(interpreter)
				.build().impactMatrix;

		// create the result
		var provider = EagerResultProvider.create(SolverContext.of(data));
		var result = new ContributionResult(provider);
		return result;
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
			if (!(c.item instanceof ImpactDescriptor))
				return null;

			var impact = (ImpactDescriptor) c.item;
			double total = result.getTotalImpactResult(impact);
			boolean withoutZeros = zeroCheck.getSelection();
			List<Contribution<?>> childs = new ArrayList<>();
			for (var flow : result.getFlows()) {
				double value = result.getDirectFlowImpact(flow, impact);
				if (value == 0 && withoutZeros)
					continue;
				Contribution<?> child = Contribution.of(flow, value);
				child.computeShare(total);
				child.unit = impact.referenceUnit;
				childs.add(child);
			}

			childs.sort((c1, c2) -> Double.compare(c2.amount, c1.amount));
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
			return c.item instanceof ImpactDescriptor;
		}
	}

	private class Label extends ColumnLabelProvider
			implements ITableLabelProvider {

		private final ContributionImage img = new ContributionImage();

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
				return c.item instanceof ImpactDescriptor
						? Images.get(ModelType.IMPACT_CATEGORY)
						: Images.get(FlowType.ELEMENTARY_FLOW);
			}
			if (col == 3 && c.item instanceof EnviFlow)
				return img.get(c.share);
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Contribution))
				return null;
			Contribution<?> c = (Contribution<?>) obj;
			switch (col) {
			case 0:
				if (c.item instanceof EnviFlow)
					return Labels.name((EnviFlow) c.item);
				if (c.item instanceof ImpactDescriptor)
					return Labels.name((ImpactDescriptor) c.item);
				return null;
			case 1:
				if (c.item instanceof EnviFlow)
					return Labels.category((EnviFlow) c.item);
				return null;
			case 2:
				if (!(c.item instanceof EnviFlow))
					return null;
				EnviFlow iFlow = (EnviFlow) c.item;
				double a = result.getTotalFlowResult(iFlow);
				return Numbers.format(a) + " " + Labels.refUnit(iFlow);
			case 3:
				return Strings.nullOrEmpty(c.unit)
						? Numbers.format(c.amount)
						: Numbers.format(c.amount) + " " + c.unit;
			default:
				return null;
			}
		}
	}
}
