package org.openlca.app.results.contributions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
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
import org.openlca.app.util.Actions;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.trees.TreeClipboard;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.commons.Strings;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.ProductSystem;
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
	private Label label;


	public ContributionTreePage(ResultEditor editor) {
		super(editor, "analysis.ContributionTreePage", M.ContributionTree);
		this.result = editor.result();
		this.setup = editor.setup();
		this.items = editor.items();
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var form = UI.header(mForm,
				Labels.name(setup.target()),
				Icon.ANALYSIS_RESULT.get());
		var body = UI.body(form, tk);
		var comp = UI.composite(body, tk);
		UI.gridLayout(comp, 2);
		var selector = ResultItemSelector
				.on(items)
				.withSelectionHandler(new SelectionHandler())
				.create(comp, tk);
		var treeComp = UI.composite(body, tk);
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
				M.RequiredAmount,
				M.TotalResult,
				M.DirectContribution
		};
		label = new Label();
		tree = Trees.createViewer(comp, headers, label);

		tree.setAutoExpandLevel(2);
		tree.getTree().setLinesVisible(false);
		tree.setContentProvider(new ContentProvider());
		tk.adapt(tree.getTree(), false, false);

		tk.paintBordersFor(tree.getTree());
		for (int i = 2; i < 5; i++) {
			tree.getTree().getColumns()[i].setAlignment(SWT.RIGHT);
		}
		Trees.bindColumnWidths(tree.getTree(),
				0.15, 0.40, 0.15, 0.15, 0.15);

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

		var onCopy = Actions.create(M.Copy, Icon.COPY.descriptor(), () -> {
			var input = tree.getInput();
			if (input instanceof UpstreamTree uTree) {
				// Get selected nodes
				var allSelected = Viewers.getAllSelected(tree);
				List<UpstreamNode> selectedNodes = new ArrayList<>();
				for (var obj : allSelected) {
					if (obj instanceof UpstreamNode node) {
						selectedNodes.add(node);
					}
				}
				
				if (selectedNodes.isEmpty()) {
					// If nothing selected, fallback to standard behavior
					TreeClipboard.onCopy(tree, new ClipboardLabel(label)).run();
				} else {
					// Generate Excel-like format for selected nodes
					var text = UpstreamTreeFormatter.generateForNodes(uTree, selectedNodes);
					var clipboard = new Clipboard(UI.shell().getDisplay());
					clipboard.setContents(
							new String[]{text},
							new Transfer[]{TextTransfer.getInstance()});
					clipboard.dispose();
				}
			} else {
				// Fallback to standard tree clipboard behavior
				TreeClipboard.onCopy(tree, new ClipboardLabel(label)).run();
			}
		});
		// Register Ctrl+C (Windows/Linux) and Command+C (Mac) for the copy action
		tree.getTree().addListener(SWT.KeyUp, (event) -> {
			if ((event.stateMask & (SWT.CTRL | SWT.MOD1)) != 0 && event.keyCode == 'c') {
				onCopy.run();
			}
		});
		Actions.bind(tree, onOpen, onCopy, onExport);
		Trees.onDoubleClick(tree, e -> onOpen.run());
	}

	private class SelectionHandler implements ResultItemSelector.SelectionHandler {

		@Override
		public void onFlowSelected(EnviFlow flow) {
			selection = flow;
			var model = UpstreamTree.of(result.provider(), flow);
			tree.setInput(model);
			updateUnit(Labels.refUnit(flow));
		}

		@Override
		public void onImpactSelected(ImpactDescriptor impact) {
			selection = impact;
			var model = UpstreamTree.of(result.provider(), impact);
			tree.setInput(model);
			updateUnit(impact.referenceUnit);
		}

		@Override
		public void onCostsSelected(CostResultDescriptor cost) {
			selection = cost;
			var model = cost.forAddedValue
					? UpstreamTree.addedValuesOf(result.provider())
					: UpstreamTree.costsOf(result.provider());
			tree.setInput(model);
			updateUnit(Labels.getReferenceCurrencyCode());
		}

		private void updateUnit(String unit) {
			var totalLabel = Strings.isNotBlank(unit)
					? M.TotalResult + " [" + unit + "]"
					: M.TotalResult;
			var directLabel = Strings.isNotBlank(unit)
					? M.DirectContribution + " [" + unit + "]"
					: M.DirectContribution;
			var t = tree.getTree();
			t.getColumn(3).setText(totalLabel);
			t.getColumn(4).setText(directLabel);
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

	}

	private class Label extends BaseLabelProvider implements ITableLabelProvider {

		private final ContributionImage image = new ContributionImage();
		private final HashMap<Long, String> groups;

		Label() {
			HashMap<Long, String> gs = null;
			if (setup.target() instanceof ProductSystem sys
					&& !sys.analysisGroups.isEmpty()) {
				gs = new HashMap<>();
				for (var g : sys.analysisGroups) {
					for (var pid : g.processes) {
						gs.put(pid, g.name);
					}
				}
			}
			groups = gs != null && !gs.isEmpty()
					? gs
					: null;
		}

		@Override
		public void dispose() {
			image.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof UpstreamNode node))
				return null;
			return switch (col) {
				case 1 -> Images.get(node.provider());
				case 3 -> image.get(shareOf(node.result()));
				case 4 -> image.get(shareOf(node.directContribution()));
				default -> null;
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof UpstreamNode node))
				return null;
			return switch (col) {
				case 0 -> Numbers.percent(shareOf(node.result()));
				case 1 -> getName(node);
				case 2 -> Numbers.format(node.requiredAmount()) + " "
						+ Labels.refUnit(node.provider());
				case 3 -> Numbers.format(node.result());
				case 4 -> {
					double d = node.directContribution();
					yield d != 0 ? Numbers.format(d) : null;
				}
				default -> null;
			};
		}

		private String getName(UpstreamNode node) {
			if (node == null || node.provider() == null)
				return null;
			var name = Labels.name(node.provider().provider());
			if (groups == null)
				return name;
			var group = groups.get(node.provider().providerId());
			return group != null
					? group + " :: " + name
					: name;
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

		private double shareOf(double result) {
			if (result == 0)
				return 0;
			double total = ((UpstreamTree) tree.getInput()).root.result();
			if (total == 0)
				return 0;
			return total < 0 && result > 0
					? -result / total
					: result / total;
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
				case 2 -> M.RequiredAmount;
				case 3 -> M.Unit;
				case 4 -> M.TotalResult + " [" + label.getUnit() + "]";
				case 5 -> M.DirectContribution + " [" + label.getUnit() + "]";
				default -> null;
			};
		}

		@Override
		public String getLabel(TreeItem item, int col) {
			if (!(item.getData() instanceof UpstreamNode node))
				return null;
			return switch (col) {
				case 0 -> Numbers.format(label.shareOf(node.result()) * 100, 2);
				case 1 -> label.getColumnText(node, 1);
				case 2 -> Double.toString(node.requiredAmount());
				case 3 -> Labels.refUnit(node.provider());
				case 4 -> Double.toString(node.result());
				case 5 -> Double.toString(node.directContribution());
				default -> null;
			};
		}
	}
}
