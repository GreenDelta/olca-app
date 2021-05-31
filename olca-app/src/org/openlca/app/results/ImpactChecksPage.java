package org.openlca.app.results;

import java.util.ArrayList;
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
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ContributionResult;

/**
 * Shows flows that are not covered by the LCIA categories of the LCIA method of
 * a result.
 */
public class ImpactChecksPage extends FormPage {

	private final ContributionResult result;

	private TreeViewer tree;

	public ImpactChecksPage(ResultEditor<?> editor) {
		super(editor, "ImpactChecksPage", M.LCIAChecks);
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
		tree.setContentProvider(new ContentProvider());
		tree.setLabelProvider(new Label());
		tree.getTree().getColumns()[2].setAlignment(SWT.RIGHT);
		Trees.bindColumnWidths(
				tree.getTree(), 0.4, 0.4, 0.2);

		// bind `onOpen`
		Action onOpen = Actions.onOpen(() -> {
			Contribution<?> c = Viewers.getFirstSelected(tree);
			if (c == null)
				return;
			if (c.item instanceof EnviFlow) {
				var f = (EnviFlow) c.item;
				App.open(f.flow());
			} else if (c.item instanceof ImpactDescriptor) {
				App.open((ImpactDescriptor) c.item);
			}
		});
		Actions.bind(tree, onOpen);
		Trees.onDoubleClick(tree, e -> onOpen.run());

		tree.setInput(group.getSelection()
				? groupedNodes()
				: flatNodes());
	}

	private List<Contribution<?>> groupedNodes() {
		return result.getImpacts()
				.stream()
				.map(Contribution::of)
				.collect(Collectors.toList());
	}

	/**
	 * Returns a flat list of flow nodes that have no characterization factor in any
	 * of the LCIA categories.
	 */
	private List<Contribution<?>> flatNodes() {
		List<Contribution<?>> nodes = new ArrayList<>();
		for (var flow : result.getFlows()) {
			boolean allZero = true;
			for (ImpactDescriptor impact : result.getImpacts()) {
				double f = result.getImpactFactor(impact, flow);
				if (f != 0) {
					allZero = false;
					break;
				}
			}
			if (allZero) {
				Contribution<?> c = Contribution.of(flow);
				c.amount = result.getTotalFlowResult(flow);
				nodes.add(c);
			}
		}
		return nodes;
	}

	private class ContentProvider extends ArrayContentProvider
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
			ImpactDescriptor impact = (ImpactDescriptor) c.item;
			c.childs = new ArrayList<>();
			for (var flow : result.getFlows()) {
				double f = result.getImpactFactor(impact, flow);
				if (f != 0)
					continue;
				Contribution<?> child = Contribution.of(flow);
				child.amount = result.getTotalFlowResult(flow);
				c.childs.add(child);
			}
			return c.childs.toArray();
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

	private static class Label extends ColumnLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col != 0 || !(obj instanceof Contribution))
				return null;
			Contribution<?> c = (Contribution<?>) obj;
			if (c.item instanceof EnviFlow)
				return Images.get((EnviFlow) c.item);
			if (c.item instanceof ImpactDescriptor)
				return Images.get((ImpactDescriptor) c.item);
			return null;
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof Contribution))
				return null;
			Contribution<?> c = (Contribution<?>) o;

			if (c.item instanceof EnviFlow) {
				var flow = (EnviFlow) c.item;
				switch (col) {
				case 0:
					return Labels.name(flow);
				case 1:
					return Labels.category(flow.flow());
				case 2:
					String unit = Labels.refUnit(flow);
					return Numbers.format(c.amount) + " " + unit;
				default:
					return null;
				}
			}

			// not a flow
			if (col != 0)
				return null;
			if (c.item instanceof ImpactDescriptor)
				return ((ImpactDescriptor) c.item).name;
			return null;
		}
	}
}
