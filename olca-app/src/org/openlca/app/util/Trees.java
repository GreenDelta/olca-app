package org.openlca.app.util;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.openlca.app.components.IModelDropHandler;
import org.openlca.app.components.ModelTransfer;
import org.openlca.core.model.descriptors.BaseDescriptor;

/**
 * A helper class for creating trees, tree viewers and related resources.
 */
public class Trees {

	public static void addDropSupport(TreeViewer tree,
			final IModelDropHandler handler) {
		final Transfer transfer = ModelTransfer.getInstance();
		DropTarget dropTarget = new DropTarget(tree.getTree(), DND.DROP_COPY
				| DND.DROP_MOVE | DND.DROP_DEFAULT);
		dropTarget.setTransfer(new Transfer[] { transfer });
		dropTarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent event) {
				if (!transfer.isSupportedType(event.currentDataType))
					return;
				List<BaseDescriptor> list = ModelTransfer
						.getBaseDescriptors(event.data);
				handler.handleDrop(list);
			}
		});
	}

	public static void bindColumnWidths(TreeViewer viewer, double... percents) {
		bindColumnWidths(viewer.getTree(), percents);
	}

	/**
	 * Binds the given percentage values (values between 0 and 1) to the column
	 * widths of the given tree
	 */
	public static void bindColumnWidths(final Tree tree,
			final double... percents) {
		if (tree == null || percents == null)
			return;
		tree.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				double width = tree.getSize().x - 25;
				if (width < 50)
					return;
				TreeColumn[] columns = tree.getColumns();
				for (int i = 0; i < columns.length; i++) {
					if (i >= percents.length)
						break;
					double colWidth = percents[i] * width;
					columns[i].setWidth((int) colWidth);
				}
			}
		});
	}

	public static <T> void makeSortable(Class<T> contentType,
			TreeViewer viewer, ITableLabelProvider labelProvider, int... cols) {
		TableColumnSorter<?>[] sorters = new TableColumnSorter<?>[cols.length];
		for (int i = 0; i < cols.length; i++) {
			sorters[i] = new TableColumnSorter<>(contentType, cols[i],
					labelProvider);
		}
		registerSorters(viewer, sorters);
	}

	public static void registerSorters(final TreeViewer viewer,
			TableColumnSorter<?>... sorters) {
		if (viewer == null || sorters == null)
			return;
		final Tree tree = viewer.getTree();
		int count = tree.getColumnCount();
		for (final TableColumnSorter<?> sorter : sorters) {
			if (sorter.getColumn() >= count)
				continue;
			final TreeColumn column = tree.getColumn(sorter.getColumn());
			column.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					TreeColumn current = tree.getSortColumn();
					if (column == current)
						sorter.setAscending(!sorter.isAscending());
					else
						sorter.setAscending(true);
					int direction = sorter.isAscending() ? SWT.UP : SWT.DOWN;
					tree.setSortDirection(direction);
					tree.setSortColumn(column);
					viewer.setSorter(sorter);
					viewer.refresh();
				}
			});
		}
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

	public static void onDeletePressed(TreeViewer viewer,
			Consumer<Event> handler) {
		if (viewer == null || viewer.getTree() == null || handler == null)
			return;
		viewer.getTree().addListener(SWT.KeyUp, (event) -> {
			if (event.keyCode == SWT.DEL) {
				handler.accept(event);
			}
		});
	}
}
