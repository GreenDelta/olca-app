package org.openlca.app.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.trees.Trees;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.matrix.DIndex;
import org.openlca.core.matrix.FlowIndex;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.ContributionResult;
import org.openlca.util.Strings;

public class ImpactChecksPage extends FormPage {

	private final ContributionResult result;
	private final CalculationSetup setup;

	private Button groupCheck;
	private TreeViewer tree;

	public ImpactChecksPage(
			FormEditor editor,
			CalculationSetup setup,
			ContributionResult result) {
		super(editor, "ImpactChecksPage", M.LCIAChecks);
		this.result = result;
		this.setup = setup;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform,
				"#Flows that are not covered by the "
						+ "selected LCIA method",
				Images.get(result));
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		groupCheck = tk.createButton(body,
				"#Group by LCIA category",
				SWT.CHECK);
		groupCheck.setSelection(true);
		Controls.onSelect(groupCheck, e -> setTreeInput());

		tree = Trees.createViewer(body,
				M.Name, M.Category, M.InventoryResult);
		UI.gridData(tree.getControl(), true, true);
		tree.setContentProvider(new Content());
		tree.setLabelProvider(new Label());
		tree.getTree().getColumns()[2].setAlignment(SWT.RIGHT);
		Trees.bindColumnWidths(
				tree.getTree(), 0.4, 0.4, 0.2);

		Action onOpen = Actions.onOpen(() -> {
			Node node = Viewers.getFirstSelected(tree);
			if (node == null)
				return;
			if (node.isFlow()) {
				App.openEditor((FlowDescriptor) node.descriptor);
			} else {
				App.openEditor(setup.impactMethod);
			}
		});
		Actions.bind(tree, onOpen);
		Trees.onDoubleClick(tree, e -> onOpen.run());

		setTreeInput();
	}

	private void setTreeInput() {
		List<Node> nodes = groupCheck.getSelection()
				? groupedNodes()
				: flatNodes();
		for (Node n : nodes) {
			if (n.childs != null) {
				n.childs.sort(this::compare);
			}
		}
		nodes.sort(this::compare);
		tree.setInput(nodes);

	}

	private List<Node> flatNodes() {
		FlowIndex flowIdx = result.flowIndex;
		DIndex<ImpactCategoryDescriptor> impactIdx = result.impactIndex;
		if (flowIdx == null || impactIdx == null
				|| impactIdx.isEmpty())
			return Collections.emptyList();
		List<Node> nodes = new ArrayList<>();
		for (int j = 0; j < flowIdx.size(); j++) {
			boolean allZero = true;
			for (int i = 0; i < impactIdx.size(); i++) {
				double f = result.impactFactors.get(i, j);
				if (f != 0) {
					allZero = false;
					break;
				}
			}
			if (allZero) {
				nodes.add(new Node(flowIdx.at(j)));
			}
		}
		return nodes;
	}

	private List<Node> groupedNodes() {
		FlowIndex flowIdx = result.flowIndex;
		DIndex<ImpactCategoryDescriptor> impactIdx = result.impactIndex;
		if (flowIdx == null || impactIdx == null
				|| impactIdx.isEmpty())
			return Collections.emptyList();
		HashMap<Integer, Node> roots = new HashMap<>();
		for (int j = 0; j < flowIdx.size(); j++) {
			for (int i = 0; i < impactIdx.size(); i++) {
				double f = result.impactFactors.get(i, j);
				if (f != 0)
					continue;
				Node root = roots.get(i);
				if (root == null) {
					root = new Node(impactIdx.at(i));
					root.childs = new ArrayList<>();
					roots.put(i, root);
				}
				root.childs.add(new Node(flowIdx.at(j)));
			}
		}
		return new ArrayList<>(roots.values());
	}

	private int compare(Node n1, Node n2) {
		String s1 = Labels.getDisplayName(n1.descriptor);
		String s2 = Labels.getDisplayName(n2.descriptor);
		return Strings.compare(s1, s2);
	}

	private class Node {
		final BaseDescriptor descriptor;
		List<Node> childs;

		Node(BaseDescriptor d) {
			this.descriptor = d;
		}

		boolean isFlow() {
			return descriptor instanceof FlowDescriptor;
		}
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

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col != 0 || !(obj instanceof Node))
				return null;
			Node n = (Node) obj;
			return Images.get(n.descriptor);
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Node))
				return null;
			Node n = (Node) obj;
			switch (col) {
			case 0:
				return Labels.getDisplayName(n.descriptor);
			case 1:
				if (n.isFlow())
					return Labels.getShortCategory(
							(FlowDescriptor) (n.descriptor));
				else
					return null;
			case 2:
				if (!n.isFlow())
					return null;
				FlowDescriptor flow = (FlowDescriptor) n.descriptor;
				double val = result.getTotalFlowResult(flow);
				String unit = Labels.getRefUnit(flow);
				return Numbers.format(val) + " " + unit;
			default:
				return null;
			}
		}
	}

}
