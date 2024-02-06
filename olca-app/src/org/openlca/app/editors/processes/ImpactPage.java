package org.openlca.app.editors.processes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.*;
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
import org.openlca.core.results.LcaResult;
import org.openlca.core.results.providers.ResultProviders;
import org.openlca.core.results.providers.SolverContext;
import org.openlca.util.Strings;

class ImpactPage extends ModelPage<Process> {

	private ImpactMethodViewer combo;
	private Button zeroCheck;
	private TreeViewer tree;
	private LcaResult result;

	ImpactPage(ProcessEditor editor) {
		super(editor, "ProcessImpactPage", M.DirectImpacts);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(this);
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		var comp = UI.composite(body, tk);
		UI.gridLayout(comp, 5);
		UI.label(comp, tk, M.ImpactAssessmentMethod);
		combo = new ImpactMethodViewer(comp);
		var methods = new ImpactMethodDao(Database.get())
				.getDescriptors()
				.stream().sorted((m1, m2) -> Strings.compare(m1.name, m2.name))
				.collect(Collectors.toList());
		combo.setInput(methods);
		combo.addSelectionChangedListener(this::setTreeInput);

		zeroCheck = UI.labeledCheckbox(comp, tk, M.ExcludeZeroValues);
		zeroCheck.setSelection(true);
		Controls.onSelect(zeroCheck, e -> setTreeInput(combo.getSelected()));

		var reload = UI.button(comp, tk, M.Reload);
		var image = Icon.REFRESH.get();
		reload.setImage(image);
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

		var onOpen = Actions.onOpen(() -> {
			Contribution<?> c = Viewers.getFirstSelected(tree);
			if (c == null)
				return;
			if (c.item instanceof EnviFlow flow) {
				App.open(flow.flow());
			}
			if (c.item instanceof ImpactDescriptor impact) {
				App.open(impact);
			}
		});
		Actions.bind(tree, onOpen);
		Trees.onDoubleClick(tree, e -> onOpen.run());

		if (!methods.isEmpty()) {
			var m = methods.get(0);
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
		var contributions = new ImpactMethodDao(Database.get())
				.getCategoryDescriptors(method.id)
				.stream()
				.sorted((d1, d2) -> Strings.compare(d1.name, d2.name))
				.map(d -> {
					var c = Contribution.of(d, result.getTotalImpactValueOf(d));
					c.unit = d.referenceUnit;
					return c;
				})
				.collect(Collectors.toList());
		tree.setInput(contributions);
	}

	private LcaResult compute() {

		var data = new MatrixData();

		// create a virtual demand of 1.0
		var refProduct = TechFlow.of(getModel());
		data.techIndex = new TechIndex(refProduct);
		data.demand = Demand.of(refProduct, 1.0);
		data.techMatrix = JavaMatrix.of(new double[][]{{1.0}});

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
			var provider = ResultProviders.solve(SolverContext.of(data));
			return new LcaResult(provider);
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
		var provider = ResultProviders.solve(SolverContext.of(data));
		return new LcaResult(provider);
	}

	private class Content extends ArrayContentProvider
			implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object obj) {
			if (!(obj instanceof Contribution<?> c))
				return null;
			if (c.childs != null)
				return c.childs.toArray();
			if (!(c.item instanceof ImpactDescriptor impact))
				return null;

			double total = result.getTotalImpactValueOf(impact);
			boolean withoutZeros = zeroCheck.getSelection();
			var childs = new ArrayList<Contribution<?>>();
			for (var flow : result.enviIndex()) {
				double value = result.getFlowImpactOf(impact, flow);
				if (value == 0 && withoutZeros)
					continue;
				var child = Contribution.of(flow, value);
				child.computeShare(total);
				child.unit = impact.referenceUnit;
				childs.add(child);
			}

			childs.sort((c1, c2) -> {
				int cc = Double.compare(c2.amount, c1.amount);
				if (cc != 0)
					return cc;
				if (!(c1.item instanceof EnviFlow flow1)
						|| !(c2.item instanceof EnviFlow flow2))
					return cc;
				return Strings.compare(Labels.name(flow1), Labels.name(flow2));
			});

			c.childs = childs;
			return childs.toArray();
		}

		@Override
		public Object getParent(Object elem) {
			return null;
		}

		@Override
		public boolean hasChildren(Object elem) {
			if (!(elem instanceof Contribution<?> c))
				return false;
			return c.childs != null || c.item instanceof ImpactDescriptor;
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
			if (!(obj instanceof Contribution<?> c))
				return null;
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
			if (!(obj instanceof Contribution<?> c))
				return null;
			switch (col) {
				case 0:
					if (c.item instanceof EnviFlow flow)
						return Labels.name(flow);
					if (c.item instanceof ImpactDescriptor impact)
						return Labels.name(impact);
					return null;
				case 1:
					return c.item instanceof EnviFlow flow
							? Labels.category(flow)
							: null;
				case 2:
					if (!(c.item instanceof EnviFlow flow))
						return null;
					double a = result.getTotalFlowValueOf(flow);
					return Numbers.format(a) + " " + Labels.refUnit(flow);
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
