package org.openlca.app.viewers.trees;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Comparator;

/**
 * A helper class for creating trees, tree viewers and related resources.
 */
public class Trees {

	public static TreeViewer createViewer(Composite parent, String... headers) {
		return createViewer(parent, headers, null);
	}

	public static TreeViewer createViewer(
			Composite parent, IBaseLabelProvider label) {
		return createViewer(parent, null, label);
	}

	public static TreeViewer createViewer(
			Composite parent, String[] headers, IBaseLabelProvider label) {
		TreeViewer viewer = new TreeViewer(parent,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.MULTI);
		Tree tree = viewer.getTree();

		tree.setHeaderBackground(Colors.listBackground());
		tree.setHeaderForeground(Colors.listForeground());
		tree.setForeground(Colors.listForeground());

		boolean hasColumns = headers != null && headers.length > 0;
		if (Display.isSystemDarkTheme())
			tree.setBackground(Colors.listBackground());
		else
			tree.setLinesVisible(hasColumns);

		tree.setHeaderVisible(hasColumns);
		if (hasColumns) {
			createColumns(viewer, headers, label);
		}
		if (label != null) {
			viewer.setLabelProvider(label);
		}
		GridData data = UI.gridData(tree, true, true);
		data.minimumHeight = 120;
		Point p = parent.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		data.heightHint = Math.max(p.y, 120);
		return viewer;
	}

	private static void createColumns(TreeViewer viewer,
			String[] labels, IBaseLabelProvider labelProvider) {
		if (labelProvider instanceof CellLabelProvider) {
			ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);
		}
		viewer.setColumnProperties(labels);
		for (String label : labels) {
			TreeViewerColumn c = new TreeViewerColumn(viewer, SWT.NULL);
			c.getColumn().setText(label);
			if (labelProvider instanceof CellLabelProvider) {
				c.setLabelProvider((CellLabelProvider) labelProvider);
			}
		}
		for (TreeColumn c : viewer.getTree().getColumns()) {
			c.pack();
		}
	}

	/**
	 * Binds the given percentage values (values between 0 and 1) to the column
	 * widths of the given tree
	 */
	public static void bindColumnWidths(Tree tree, double... percents) {
		bindColumnWidths(tree, 0, percents);
	}

	public static void bindColumnWidths(Tree tree, int minimum, double... percents) {
		if (tree == null || percents == null)
			return;
		var treeListener = new TreeResizeListener(tree, minimum, percents);
		var columnListener = new ColumnResizeListener(treeListener);
		for (var column : tree.getColumns()) {
			column.addControlListener(columnListener);
		}
		tree.addControlListener(treeListener);
	}

	/** Add an event handler for double clicks on the given tree viewer. */
	public static void onDoubleClick(TreeViewer viewer,
			Consumer<MouseEvent> handler) {
		if (viewer == null || viewer.getTree() == null || handler == null)
			return;
		viewer.getTree().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				handler.accept(e);
			}
		});
	}

	/**
	 * Get the tree item where the given event occurred. Returns null if the
	 * event occurred in the empty tree area.
	 */
	public static TreeItem getItem(TreeViewer viewer, MouseEvent event) {
		if (viewer == null || event == null)
			return null;
		Tree tree = viewer.getTree();
		if (tree == null)
			return null;
		return tree.getItem(new Point(event.x, event.y));
	}

	public static void addComparator(TreeViewer viewer, Comparator<?> comparator) {
		Tree tree = viewer.getTree();
		if (comparator.column >= tree.getColumnCount())
			return;
		TreeColumn column = tree.getColumn(comparator.column);
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeColumn current = tree.getSortColumn();
				if (column == current)
					comparator.ascending = !comparator.ascending;
				else
					comparator.ascending = true;
				int direction = comparator.ascending ? SWT.UP : SWT.DOWN;
				tree.setSortDirection(direction);
				tree.setSortColumn(column);
				viewer.setComparator(comparator);
				viewer.refresh();
			}
		});
	}

	// In order to be able to resize columns manually, we must know if a column
	// was resized before, and in those cases, don't resize the columns
	// automatically.
	private static class ColumnResizeListener extends ControlAdapter {
		private final TreeResizeListener depending;
		private boolean enabled = true;
		private boolean initialized;

		private ColumnResizeListener(TreeResizeListener depending) {
			this.depending = depending;
		}

		@Override
		public void controlResized(ControlEvent e) {
			if (!enabled)
				return;
			if (!initialized) {
				initialized = true;
				return;
			}
			depending.enabled = false;
			enabled = false;
			Timer t = new Timer();
			t.schedule(new TimerTask() {
				@Override
				public void run() {
					depending.enabled = true;
					enabled = true;
				}
			}, 100);
		}
	}

	private static class TreeResizeListener extends ControlAdapter {
		private final Tree tree;
		private final double[] percents;
		private final int minimum;
		private boolean enabled = true;
		private boolean initialized;

		private TreeResizeListener(Tree tree, int minimum, double[] percents) {
			this.tree = tree;
			this.minimum = minimum;
			this.percents = percents;
		}

		@Override
		public void controlResized(ControlEvent e) {
			if (!enabled && initialized)
				return;
			double width = tree.getSize().x - 25;
			if (width <= 0)
				return;
			TreeColumn[] columns = tree.getColumns();
			int indexOfLargest = -1;
			double max = 0;
			double diff = 0;
			for (int i = 0; i < columns.length; i++) {
				if (i >= percents.length)
					break;
				double colWidth = percents[i] * width;
				if (max < colWidth) {
					max = colWidth;
					indexOfLargest = i;
				}
				if (colWidth < minimum) {
					colWidth = minimum;
					diff += minimum - colWidth;
				}
				columns[i].setWidth((int) colWidth);
			}
			if (diff > 0) {
				columns[indexOfLargest].setWidth((int) (max - diff));
			}
			initialized = true;
		}

	}
}
