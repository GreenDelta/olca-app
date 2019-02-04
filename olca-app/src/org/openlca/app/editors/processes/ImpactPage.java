package org.openlca.app.editors.processes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.db.Cache;
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
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.math.ReferenceAmount;
import org.openlca.core.matrix.DIndex;
import org.openlca.core.matrix.FlowIndex;
import org.openlca.core.matrix.ImpactTable;
import org.openlca.core.matrix.ParameterTable;
import org.openlca.core.matrix.format.IMatrix;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

class ImpactPage extends ModelPage<Process> {

	private Button zeroCheck;
	private TreeViewer tree;

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
			Node node = Viewers.getFirstSelected(tree);
			if (node == null)
				return;
			if (node.exchange != null) {
				App.openEditor(node.exchange.flow);
			} else if (node.impact != null) {
				App.openEditor(combo.getSelected());
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
		AtomicReference<List<Node>> ref = new AtomicReference<>();
		boolean skipZeros = zeroCheck.getSelection();
		App.runWithProgress("Load tree", () -> {
			ref.set(buildTree(method, skipZeros));
		}, () -> {
			List<Node> nodes = ref.get();
			if (nodes != null) {
				tree.setInput(nodes);
			}
		});
	}

	private List<Node> buildTree(ImpactMethodDescriptor method,
			boolean skipZeros) {
		if (method == null)
			return Collections.emptyList();

		// index the elementary flows
		List<Exchange> eList = getModel().exchanges;
		double[] values = new double[eList.size()];
		Exchange[] exchanges = new Exchange[eList.size()];
		FlowIndex flowIdx = new FlowIndex();
		for (Exchange e : eList) {
			if (e.flow == null ||
					e.flow.flowType != FlowType.ELEMENTARY_FLOW)
				continue;
			FlowDescriptor d = Descriptors.toDescriptor(e.flow);
			int i = e.isInput
					? flowIdx.putInput(d)
					: flowIdx.putOutput(d);
			exchanges[i] = e;
			values[i] = ReferenceAmount.get(e);
		}

		// create the impact matrix
		FormulaInterpreter interpreter = ParameterTable.interpreter(
				Database.get(),
				new HashSet<Long>(Arrays.asList(
						getModel().id,
						method.id)),
				Collections.emptySet());
		ImpactTable iTable = ImpactTable.build(
				Cache.getMatrixCache(), method.id, flowIdx);
		IMatrix matrix = iTable.createMatrix(
				App.getSolver(), interpreter);

		// build the tree
		List<Node> roots = new ArrayList<>();
		DIndex<ImpactCategoryDescriptor> impactIdx = iTable.impactIndex;
		for (int i = 0; i < impactIdx.size(); i++) {
			Node root = new Node();
			root.impact = impactIdx.at(i);
			roots.add(root);
			for (int j = 0; j < flowIdx.size(); j++) {
				double factor = matrix.get(i, j);
				if (exchanges[j].isInput) {
					factor = -factor;
				}
				double result = values[j] * factor;
				if (result == 0 && skipZeros)
					continue;
				root.result += result;
				Node child = new Node();
				child.result = result;
				child.exchange = exchanges[j];
				child.impact = root.impact;
				if (root.childs == null) {
					root.childs = new ArrayList<>();
				}
				root.childs.add(child);
			}
		}
		sort(roots);
		return roots;
	}

	private void sort(List<Node> roots) {
		Collections.sort(roots, (n1, n2) -> {
			String l1 = Labels.getDisplayName(n1.impact);
			String l2 = Labels.getDisplayName(n2.impact);
			return Strings.compare(l1, l2);
		});
		for (Node root : roots) {
			if (root.childs == null)
				continue;
			if (root.result != 0) {
				for (Node child : root.childs) {
					child.share = child.result / root.result;
				}
			}
			Collections.sort(root.childs, (n1, n2) -> {
				int c = Double.compare(n2.share, n1.share);
				if (c != 0)
					return c;
				if (n1.exchange == null || n2.exchange == null)
					return c;
				String l1 = Labels.getDisplayName(n1.exchange.flow);
				String l2 = Labels.getDisplayName(n2.exchange.flow);
				return Strings.compare(l1, l2);
			});
		}
	}

	private class Node {
		ImpactCategoryDescriptor impact;
		double share;
		double result;
		Exchange exchange;
		List<Node> childs;
	}

	private class Content extends ArrayContentProvider
			implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object parent) {
			if (!(parent instanceof Node))
				return null;
			Node n = (Node) parent;
			return n.childs == null ? null : n.childs.toArray();
		}

		@Override
		public Object getParent(Object elem) {
			return null;
		}

		@Override
		public boolean hasChildren(Object elem) {
			if (!(elem instanceof Node))
				return false;
			Node n = (Node) elem;
			return n.childs != null && n.childs.size() > 0;
		}
	}

	private class Label extends ColumnLabelProvider
			implements ITableLabelProvider {

		private ContributionImage img = new ContributionImage(Display.getCurrent());

		@Override
		public void dispose() {
			img.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Node))
				return null;
			Node n = (Node) obj;
			if (col == 0) {
				if (n.exchange == null)
					return Images.get(ModelType.IMPACT_CATEGORY);
				else
					return Images.get(FlowType.ELEMENTARY_FLOW);
			}
			if (col != 3 || n.exchange == null)
				return null;
			return img.getForTable(n.share);
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Node))
				return null;
			Node n = (Node) obj;
			switch (col) {
			case 0:
				if (n.exchange == null)
					return Labels.getDisplayName(n.impact);
				else
					return Labels.getDisplayName(n.exchange.flow);
			case 1:
				if (n.exchange == null)
					return null;
				else
					return CategoryPath.getShort(
							n.exchange.flow.category);
			case 2:
				if (n.exchange == null)
					return null;
				if (n.exchange.unit == null)
					return Numbers.format(n.exchange.amount);
				else
					return Numbers.format(n.exchange.amount)
							+ " " + n.exchange.unit.name;
			case 3:
				if (n.impact == null)
					return null;
				if (n.impact.referenceUnit == null)
					return Numbers.format(n.result);
				else
					return Numbers.format(n.result)
							+ " " + n.impact.referenceUnit;
			default:
				return null;
			}
		}
	}
}
