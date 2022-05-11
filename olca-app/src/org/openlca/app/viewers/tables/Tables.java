package org.openlca.app.viewers.tables;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Comparator;
import org.openlca.util.Strings;

/**
 * A helper class for creating tables, table viewers and related resources.
 */
public class Tables {

	public static TableViewer createViewer(Composite parent, String... properties) {
		return createViewer(parent, properties, (IBaseLabelProvider) null);
	}

	/**
	 * Creates a default table viewer with the given properties. The properties
	 * are also used to create columns where each column label is the respective
	 * property of this column. The viewer is configured in the following way:
	 * <ul>
	 * <li>content provider = {@link ArrayContentProvider}
	 * <li>lines and header are visible
	 * <li>grid data with horizontal and vertical fill
	 *
	 */
	public static TableViewer createViewer(Composite parent, String[] properties,
			IBaseLabelProvider labelProvider) {
		boolean hasColumns = properties != null && properties.length > 0;
		var viewer = createViewer(parent, hasColumns);
		if (hasColumns) {
			createColumns(viewer, properties, i -> labelProvider);
		}
		if (labelProvider != null) {
			viewer.setLabelProvider(labelProvider);
		}
		return viewer;
	}

	public static TableViewer createViewer(Composite parent, String[] properties,
			Function<Integer, IBaseLabelProvider> labelProviders) {
		var viewer = createViewer(parent, true);
		createColumns(viewer, properties, labelProviders);
		return viewer;
	}

	private static TableViewer createViewer(Composite parent, boolean hasColumns) {
		var viewer = new TableViewer(parent,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.MULTI);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		Table table = viewer.getTable();
		table.setLinesVisible(hasColumns);
		table.setHeaderVisible(hasColumns);
		var data = UI.gridData(table, true, true);
		data.minimumHeight = 120;
		// workaround for this bug:
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=215997
		Point p = parent.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		data.heightHint = Math.max(p.y, 120);
		return viewer;
	}

	private static void createColumns(TableViewer viewer, String[] labels,
			Function<Integer, IBaseLabelProvider> labelProviders) {
		var labelProvider = labelProviders != null ? labelProviders.apply(0) : null;
		if (labelProvider instanceof CellLabelProvider) {
			ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);
		}
		viewer.setColumnProperties(labels);
		for (var i = 0; i < labels.length; i++) {
			var label = labels[i];
			labelProvider = labelProviders != null ? labelProviders.apply(i) : null;
			var c = new TableViewerColumn(viewer, SWT.NULL);
			c.getColumn().setText(Strings.orEmpty(label));
			if (labelProvider instanceof CellLabelProvider) {
				c.setLabelProvider((CellLabelProvider) labelProvider);
			}
		}
		for (TableColumn c : viewer.getTable().getColumns())
			c.pack();
	}

	public static void bindColumnWidths(TableViewer viewer, double... percents) {
		bindColumnWidths(viewer.getTable(), percents);
	}

	public static void bindColumnWidths(TableViewer viewer, int minimum, double... percents) {
		bindColumnWidths(viewer.getTable(), minimum, percents);
	}

	/**
	 * Binds the given percentage values (values between 0 and 1) to the column
	 * widths of the given table
	 */
	public static void bindColumnWidths(Table table, double... percents) {
		bindColumnWidths(table, 0, percents);
	}

	public static void bindColumnWidths(Table table, int minimum, double... percents) {
		if (table == null || percents == null)
			return;
		var tabResizer = new TableResizeListener(table, percents, minimum);
		// see resize listener declaration for comment on why this is done
		var colResizer = new ColumnResizeListener(tabResizer);
		for (var column : table.getColumns()) {
			column.addControlListener(colResizer);
		}
		table.addControlListener(tabResizer);
	}

	/** Add an event handler for double clicks on the given table viewer. */
	public static void onDoubleClick(TableViewer viewer, Consumer<MouseEvent> handler) {
		if (viewer == null || viewer.getTable() == null || handler == null)
			return;
		viewer.getTable().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				handler.accept(e);
			}
		});
	}

	/**
	 * Get the table item where the given event occurred. Returns null if the
	 * event occurred in the empty table area.
	 */
	public static TableItem getItem(TableViewer viewer, MouseEvent event) {
		if (viewer == null || event == null)
			return null;
		Table table = viewer.getTable();
		if (table == null)
			return null;
		return table.getItem(new Point(event.x, event.y));
	}

	public static void onDeletePressed(TableViewer viewer, Consumer<Event> handler) {
		if (viewer == null || viewer.getTable() == null || handler == null)
			return;
		viewer.getTable().addListener(SWT.KeyUp, (event) -> {
			if (event.keyCode == SWT.DEL) {
				handler.accept(event);
			}
		});
	}

	public static void addComparator(TableViewer viewer, Comparator<?> comparator) {
		Table table = viewer.getTable();
		if (comparator.column >= table.getColumnCount())
			return;
		TableColumn column = table.getColumn(comparator.column);
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableColumn current = table.getSortColumn();
				if (column == current)
					comparator.ascending = !comparator.ascending;
				else
					comparator.ascending = true;
				int direction = comparator.ascending ? SWT.UP : SWT.DOWN;
				table.setSortDirection(direction);
				table.setSortColumn(column);
				viewer.setComparator(comparator);
				viewer.refresh();
			}
		});
	}

	// In order to be able to resize columns manually, we must know if a column
	// was resized before, and in those cases, don't resize the columns
	// automatically.
	private static class ColumnResizeListener extends ControlAdapter {
		private final TableResizeListener depending;
		private boolean enabled = true;
		private boolean initialized;

		private ColumnResizeListener(TableResizeListener depending) {
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

	private static class TableResizeListener extends ControlAdapter {
		private final Table table;
		private final double[] percents;
		private final int minimum;
		private boolean enabled = true;

		private TableResizeListener(Table table, double[] percents, int mininmum) {
			this.table = table;
			this.percents = percents;
			this.minimum = mininmum;
		}

		@Override
		public void controlResized(ControlEvent e) {
			if (!enabled)
				return;
			double width = table.getSize().x - 25;
			TableColumn[] columns = table.getColumns();
			int longest = -1;
			double max = 0;
			double additional = 0;
			for (int i = 0; i < columns.length; i++) {
				if (i >= percents.length)
					break;
				double colWidth = percents[i] * width;
				if (max < colWidth) {
					max = colWidth;
					longest = i;
				}
				if (minimum > 0 && colWidth < minimum) {
					additional += minimum - colWidth;
					colWidth = minimum;
				}
				if (colWidth == 0)
					continue;
				columns[i].setWidth((int) colWidth);
			}
			if (additional == 0 || longest == -1)
				return;
			columns[longest].setWidth((int) (percents[longest] * width - additional));
		}

	}

}
