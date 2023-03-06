package org.openlca.app.results.contributions;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.components.ResultItemSelector;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.util.*;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.trees.TreeClipboard;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.LcaResult;
import org.openlca.core.results.ResultItemOrder;
import org.openlca.core.results.UpstreamNode;
import org.openlca.core.results.UpstreamTree;

public class ContributionTreePage extends FormPage {

	private final LcaResult result;
	private final CalculationSetup setup;
	private final ResultItemOrder items;

	private TreeViewer tree;
	private Object selection;


	public ContributionTreePage(ResultEditor editor) {
		super(editor, "analysis.ContributionTreePage", M.ContributionTree);
		this.result = editor.result;
		this.setup = editor.setup;
		this.items = editor.items;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var form = UI.formHeader(mForm,
				Labels.name(setup.target()),
				Icon.ANALYSIS_RESULT.get());
		var body = UI.formBody(form, tk);
		var comp = UI.formComposite(body, tk);
		UI.gridLayout(comp, 2);
		var selector = ResultItemSelector
				.on(items)
				.withSelectionHandler(new SelectionHandler())
				.create(comp, tk);
		var treeComp = UI.formComposite(body, tk);
		UI.gridLayout(treeComp, 1);
		UI.gridData(treeComp, true, true);
		createTree(tk, treeComp);
		form.reflow(true);
		selector.initWithEvent();
	}

	private void createTree(FormToolkit tk, Composite comp) {
		var headers = new String[]{
				M.Contribution,
				M.Process,
				"Required amount",
				M.Result};
		var label = new Label();
		tree = Trees.createViewer(comp, headers, label);

		tree.setAutoExpandLevel(2);
		tree.getTree().setLinesVisible(false);
		tree.setContentProvider(new ContentProvider());
		tk.adapt(tree.getTree(), false, false);
		tree.getTree().setBackground(Colors.formBackground());
		tree.getTree().setForeground(Colors.formForeground());

		tk.paintBordersFor(tree.getTree());
		tree.getTree().getColumns()[2].setAlignment(SWT.RIGHT);
		tree.getTree().getColumns()[3].setAlignment(SWT.RIGHT);
		Trees.bindColumnWidths(tree.getTree(),
				0.20, 0.40, 0.20, 0.20);

		// action bindings
		var onOpen = Actions.onOpen(() -> {
			UpstreamNode n = Viewers.getFirstSelected(tree);
			if (n == null || n.provider() == null)
				return;
			App.open(n.provider().provider());
		});

		var onExport = Actions.create(M.ExportToExcel,
				Images.descriptor(FileType.EXCEL), () -> {
					if (tree.getInput() instanceof UpstreamTree uTree) {
						TreeExportDialog.open(uTree);
					}
				});

		var onCopy = TreeClipboard.onCopy(tree, new ClipboardLabel(label));
		Actions.bind(tree, onOpen, onCopy, onExport);
		Trees.onDoubleClick(tree, e -> onOpen.run());
	}

	private class SelectionHandler implements ResultItemSelector.SelectionHandler {

		@Override
		public void onFlowSelected(EnviFlow flow) {
			selection = flow;
			var model = UpstreamTree.of(result.provider(), flow);
			tree.setInput(model);
		}

		@Override
		public void onImpactSelected(ImpactDescriptor impact) {
			selection = impact;
			var model = UpstreamTree.of(result.provider(), impact);
			tree.setInput(model);
		}

		@Override
		public void onCostsSelected(CostResultDescriptor cost) {
			selection = cost;
			var model = cost.forAddedValue
					? UpstreamTree.addedValuesOf(result.provider())
					: UpstreamTree.costsOf(result.provider());
			tree.setInput(model);
		}
	}

	private static class ContentProvider implements ITreeContentProvider {

		private UpstreamTree tree;

		@Override
		public Object[] getChildren(Object parent) {
			if (!(parent instanceof UpstreamNode node))
				return null;
			if (tree == null)
				return null;
			return tree.childs(node).toArray();
		}

		@Override
		public Object[] getElements(Object input) {
			return input instanceof UpstreamTree t
					? new Object[]{t.root}
					: null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object elem) {
			if (!(elem instanceof UpstreamNode node))
				return false;
			return !tree.childs(node).isEmpty();
		}

		@Override
		public void inputChanged(Viewer viewer, Object old, Object input) {
			if (!(input instanceof UpstreamTree)) {
				tree = null;
				return;
			}
			tree = (UpstreamTree) input;
		}

		@Override
		public void dispose() {
		}

	}

	private class Label extends BaseLabelProvider implements ITableLabelProvider {

		private final ContributionImage image = new ContributionImage();

		@Override
		public void dispose() {
			image.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof UpstreamNode node))
				return null;
			if (col == 1 && node.provider() != null)
				return Images.get(node.provider().provider());
			if (col == 3)
				return image.get(getContribution(node));
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof UpstreamNode node))
				return null;
			return switch (col) {
				case 0 -> Numbers.percent(getContribution(node));
				case 1 -> Labels.name(node.provider().provider());
				case 2 -> Numbers.format(node.requiredAmount()) + " "
						+ Labels.refUnit(node.provider());
				case 3 -> Numbers.format(node.result()) + " " + getUnit();
				default -> null;
			};
		}

		private String getUnit() {
			if (selection instanceof EnviFlow flow) {
				return Labels.refUnit(flow);
			} else if (selection instanceof ImpactDescriptor impact) {
				return impact.referenceUnit;
			} else if (selection instanceof CostResultDescriptor) {
				return Labels.getReferenceCurrencyCode();
			}
			return null;
		}

		private double getContribution(UpstreamNode node) {
			if (node.result() == 0)
				return 0;
			double total = ((UpstreamTree) tree.getInput()).root.result();
			if (total == 0)
				return 0;
			return total < 0 && node.result() > 0
					? -node.result() / total
					: node.result() / total;
		}
	}

	private record ClipboardLabel(Label label) implements TreeClipboard.Provider {

		@Override
		public int columns() {
			return 6;
		}

		@Override
		public String getHeader(int col) {
			return switch (col) {
				case 0 -> M.Contribution + " [%]";
				case 1 -> M.Process;
				case 2 -> "Required amount";
				case 3, 5 -> M.Unit;
				case 4 -> M.Result;
				default -> null;
			};
		}

		@Override
		public String getLabel(TreeItem item, int col) {
			if (!(item.getData() instanceof UpstreamNode node))
				return null;
			return switch (col) {
				case 0 -> Numbers.format(label.getContribution(node) * 100, 2);
				case 1 -> label.getColumnText(node, 1);
				case 2 -> Double.toString(node.requiredAmount());
				case 3 -> Labels.refUnit(node.provider());
				case 4 -> Double.toString(node.result());
				case 5 -> label.getUnit();
				default -> null;
			};
		}
	}
}
