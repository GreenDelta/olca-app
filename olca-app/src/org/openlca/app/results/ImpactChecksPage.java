package org.openlca.app.results;

import java.util.ArrayList;
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
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.ContributionResult;

/**
 * Shows flows that are not covered by the LCIA categories of the LCIA method of
 * a result.
 */
public class ImpactChecksPage extends FormPage {

	private final ResultEditor<?> editor;
	private final ContributionResult result;

	private TreeViewer tree;

	public ImpactChecksPage(ResultEditor<?> editor) {
		super(editor, "ImpactChecksPage", M.LCIAChecks);
		this.editor = editor;
		this.result = editor.result;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform,
				"Flows that are not covered by the "
						+ "selected LCIA method",
				Images.get(result));
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);

		// the grouping check
		Button group = tk.createButton(body,
				"Group by LCIA category", SWT.CHECK);
		group.setSelection(true);
		Controls.onSelect(group, e -> {
			tree.setInput(
					group.getSelection()
							? groupedNodes()
							: flatNodes());
		});

		// create the tree
		tree = Trees.createViewer(body,
				M.Name, M.Category, M.InventoryResult);
		UI.gridData(tree.getControl(), true, true);
		tree.setContentProvider(new ContentProvider());
		tree.setLabelProvider(new Label());
		tree.getTree().getColumns()[2].setAlignment(SWT.RIGHT);
		Trees.bindColumnWidths(
				tree.getTree(), 0.4, 0.4, 0.2);

		// bind `onOpen`
		Action onOpen = Actions.onOpen(() -> {
			Node node = Viewers.getFirstSelected(tree);
			if (node == null)
				return;
			IndexFlow flow = node.flow();
			if (flow != null) {
				App.openEditor(flow.flow);
				return;
			}
			ImpactCategoryDescriptor impact = node.impact();
			if (impact != null) {
				App.openEditor(impact);
			}
		});
		Actions.bind(tree, onOpen);
		Trees.onDoubleClick(tree, e -> onOpen.run());

		tree.setInput(group.getSelection()
				? groupedNodes()
				: flatNodes());
	}

	/**
	 * Returns a flat list of flow nodes that have no characterization factor in any
	 * of the LCIA categories.
	 */
	private List<Node> flatNodes() {
		List<Node> nodes = new ArrayList<>();
		for (IndexFlow flow : result.getFlows()) {
			boolean allZero = true;
			for (ImpactCategoryDescriptor impact : result.getImpacts()) {
				double f = result.getImpactFactor(impact, flow);
				if (f != 0) {
					allZero = false;
					break;
				}
			}
			if (allZero) {
				nodes.add(new Node(flow));
			}
		}
		return nodes;
	}

	private List<Node> groupedNodes() {
		List<Node> nodes = new ArrayList<>();
		for (ImpactCategoryDescriptor impact : result.getImpacts()) {
			Node impactNode = new Node(impact);
			nodes.add(impactNode);
			for (IndexFlow flow : result.getFlows()) {
				double f = result.getImpactFactor(impact, flow);
				if (f != 0)
					continue;
				if (impactNode.childs == null) {
					impactNode.childs = new ArrayList<>();
				}
				impactNode.childs.add(new Node(flow));
			}
		}
		return nodes;
	}

	private class Node {
		final Object content;
		List<Node> childs;

		Node(Object d) {
			this.content = d;
		}

		IndexFlow flow() {
			return content instanceof IndexFlow
					? (IndexFlow) content
					: null;
		}

		ImpactCategoryDescriptor impact() {
			return content instanceof ImpactCategoryDescriptor
					? (ImpactCategoryDescriptor) content
					: null;
		}

	}

	private class ContentProvider extends ArrayContentProvider
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
			IndexFlow flow = n.flow();
			if (flow != null)
				return Images.get(flow.flow);
			ImpactCategoryDescriptor impact = n.impact();
			if (impact != null)
				return Images.get(impact);
			return null;
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof Node))
				return null;
			Node n = (Node) o;

			IndexFlow flow = n.flow();
			if (flow != null) {
				switch (col) {
				case 0:
					return Labels.name(flow);
				case 1:
					return Labels.category(flow.flow);
				case 2:
					double val = result.getTotalFlowResult(flow);
					String unit = Labels.refUnit(flow);
					return Numbers.format(val) + " " + unit;
				default:
					return null;
				}
			}

			ImpactCategoryDescriptor impact = n.impact();
			if (impact == null || col != 0)
				return null;
			return impact.name;
		}
	}
}
