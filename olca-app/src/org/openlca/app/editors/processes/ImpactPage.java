package org.openlca.app.editors.processes;

import java.util.ArrayList;
import java.util.Collections;
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
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.trees.TreeClipboard;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.LcaResult;
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
		Actions.bind(tree, onOpen, TreeClipboard.onCopy(tree));
		Trees.onDoubleClick(tree, e -> onOpen.run());

		if (!methods.isEmpty()) {
			var m = methods.getFirst();
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
			App.runInUI(M.ComputeLciaResultsDots,
					() -> {
				var res = DirectProcessResult.calculate(getModel());
				if (res.hasError()) {
					MsgBox.error("Calculation failed", res.error());
				} else {
					result = res.value();
					setTreeInput(method);
				}
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
				return c.item instanceof ImpactDescriptor d
						? Images.get(d)
						: Images.get(FlowType.ELEMENTARY_FLOW);
			}
			return col == 3 && c.item instanceof EnviFlow
					? img.get(c.share)
					: null;
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
