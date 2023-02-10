package org.openlca.app.results;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.trees.TreeClipboard;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.LcaResult;
import org.openlca.core.results.ResultItemOrder;

/**
 * Shows flows that are not covered by the LCIA categories of the LCIA method of
 * a result.
 */
public class ImpactChecksPage extends FormPage {

	private final LcaResult result;
	private final ResultItemOrder items;

	private TreeViewer tree;

	public ImpactChecksPage(ResultEditor editor) {
		super(editor, "ImpactChecksPage", M.LCIAChecks);
		this.result = editor.result;
		this.items = editor.items;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.formHeader(mForm,
				"Flows that are not covered by the "
						+ "selected LCIA method",
				Icon.ANALYSIS_RESULT.get());
		var tk = mForm.getToolkit();
		var body = UI.formBody(form, tk);

		// the grouping check
		var group = tk.createButton(body,
				"Group by LCIA category", SWT.CHECK);
		group.setSelection(true);
		Controls.onSelect(group, e -> tree.setInput(
				group.getSelection()
						? groupedNodes()
						: flatNodes()));

		// create the tree
		tree = Trees.createViewer(body,
				M.Name, M.Category, M.InventoryResult);
		tree.setContentProvider(new ContentProvider());
		var label = new Label();
		tree.setLabelProvider(label);
		tree.getTree().getColumns()[2].setAlignment(SWT.RIGHT);
		Trees.bindColumnWidths(
				tree.getTree(), 0.4, 0.4, 0.2);

		// bind `onOpen`
		var onOpen = Actions.onOpen(() -> {
			Contribution<?> c = Viewers.getFirstSelected(tree);
			if (c == null)
				return;
			if (c.item instanceof EnviFlow f) {
				App.open(f.flow());
			} else if (c.item instanceof ImpactDescriptor i) {
				App.open(i);
			}
		});
		var onCopy = TreeClipboard.onCopy(tree, new ClipboardLabel(label));
		Actions.bind(tree, onOpen, onCopy);
		Trees.onDoubleClick(tree, e -> onOpen.run());

		tree.setInput(group.getSelection()
				? groupedNodes()
				: flatNodes());
	}

	private List<Contribution<?>> groupedNodes() {
		return items.impacts()
				.stream()
				.map(Contribution::of)
				.collect(Collectors.toList());
	}

	/**
	 * Returns a flat list of flow nodes that have no characterization factor in any
	 * of the LCIA categories.
	 */
	private List<Contribution<?>> flatNodes() {
		var nodes = new ArrayList<Contribution<?>>();
		for (var flow : items.enviFlows()) {
			boolean allZero = true;
			for (var impact : items.impacts()) {
				double f = result.getImpactFactorOf(impact, flow);
				if (f != 0) {
					allZero = false;
					break;
				}
			}
			if (allZero) {
				Contribution<?> c = Contribution.of(flow);
				c.amount = result.getTotalFlowValueOf(flow);
				nodes.add(c);
			}
		}
		return nodes;
	}

	private class ContentProvider extends ArrayContentProvider
			implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object obj) {
			if (!(obj instanceof Contribution<?> c))
				return null;
			if (c.childs != null)
				return c.childs.toArray();
			if (!(c.item instanceof ImpactDescriptor impact))
				return null;
			c.childs = new ArrayList<>();
			for (var flow : items.enviFlows()) {
				double f = result.getImpactFactorOf(impact, flow);
				if (f != 0)
					continue;
				Contribution<?> child = Contribution.of(flow);
				child.amount = result.getTotalFlowValueOf(flow);
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
			if (!(elem instanceof Contribution<?> c))
				return false;
			if (c.childs != null)
				return true;
			return c.item instanceof ImpactDescriptor;
		}
	}

	private record ClipboardLabel(Label label) implements TreeClipboard.Provider {

		@Override
		public int columns() {
			return 4;
		}

		@Override
		public String getHeader(int col) {
			return switch (col) {
				case 0 -> M.Name;
				case 1 -> M.Category;
				case 2 -> M.InventoryResult;
				case 3 -> M.Unit;
				default -> null;
			};
		}

		@Override
		public String getLabel(TreeItem item, int col) {
			if (!(item.getData() instanceof Contribution<?> c))
				return null;
			boolean isFlow = c.item instanceof EnviFlow;
			return switch (col) {
				case 0, 1 -> label.getColumnText(c, col);
				case 2 -> isFlow
						? Double.toString(c.amount)
						: null;
				case 3 -> isFlow
						? Labels.refUnit((EnviFlow) c.item)
						: null;
				default -> null;
			};
		}
	}

	private static class Label extends ColumnLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col != 0 || !(obj instanceof Contribution<?> c))
				return null;
			if (c.item instanceof EnviFlow)
				return Images.get((EnviFlow) c.item);
			if (c.item instanceof ImpactDescriptor)
				return Images.get((ImpactDescriptor) c.item);
			return null;
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof Contribution<?> c))
				return null;

			if (c.item instanceof EnviFlow flow) {
				return switch (col) {
					case 0 -> Labels.name(flow);
					case 1 -> Labels.category(flow);
					case 2 -> Numbers.format(c.amount) + " " + Labels.refUnit(flow);
					default -> null;
				};
			}

			// not a flow
			if (col != 0)
				return null;
			return c.item instanceof ImpactDescriptor indicator
					? indicator.name
					: null;
		}
	}
}
