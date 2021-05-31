package org.openlca.app.viewers.tables;

import java.util.function.Consumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;

public final class TableClipboard {

	private TableClipboard() {
	}

	/**
	 * Registers Ctr+c for copying the selected items of the given table to the
	 * clipboard and also returns an action for this.
	 */
	public static Action onCopySelected(TableViewer viewer) {
		return new CopyAction(viewer.getTable(), TableClipboard::text, true);
	}

	/**
	 * Same as {@link #onCopySelected(TableViewer)} but with an additional converter
	 * for transforming a table item into a string representation.
	 */
	public static Action onCopySelected(TableViewer viewer, Converter converter) {
		return new CopyAction(viewer.getTable(), converter, true);
	}

	/**
	 * Returns an action for copying the full table content to the clipboard. It
	 * does not registers Ctrl+c bindings for this.
	 */
	public static Action onCopyAll(TableViewer viewer) {
		return new CopyAction(viewer.getTable(), TableClipboard::text, false);
	}

	/**
	 * Same as {@link #onCopyAll(TableViewer)} but with an additional converter for
	 * transforming a table item into a string representation.
	 */
	public static Action onCopyAll(TableViewer viewer, Converter converter) {
		return new CopyAction(viewer.getTable(), converter, false);
	}

	/**
	 * Registers Ctr+v for pasting table content from clipboard and returns an
	 * action which also calls this function.
	 */
	public static Action onPaste(TableViewer viewer, Consumer<String> fn) {
		return onPaste(viewer.getTable(), fn);
	}

	private static Action onPaste(Table table, Consumer<String> fn) {
		table.addListener(SWT.KeyUp, e -> {
			if (((e.stateMask & SWT.CTRL) == SWT.CTRL)
					&& (e.keyCode == 'v' || e.keyCode == 'V')) {
				paste(table, fn);
			}
		});
		var image = Icon.PASTE.descriptor();
		return Actions.create(M.Paste, image, () -> paste(table, fn));
	}

	private static void paste(Table table, Consumer<String> fn) {
		if (table == null || fn == null)
			return;
		Clipboard clipboard = new Clipboard(UI.shell().getDisplay());
		try {
			TextTransfer transfer = TextTransfer.getInstance();
			Object content = clipboard.getContents(transfer);
			if (!(content instanceof String))
				return;
			fn.accept((String) content);
		} finally {
			clipboard.dispose();
		}
	}

	/**
	 * A converter function returns the string presentation of a given column for a
	 * table item. This function can be used to write specific converter functions
	 * for table clipboards.
	 */
	@FunctionalInterface
	public interface Converter {
		String asString(TableItem item, int col);
	}

	/**
	 * Returns the text of the table item in the given column.
	 */
	public static String text(TableItem item, int col) {
		if (item == null || col < 0)
			return "";
		String s = item.getText(col).replaceAll("\\t", "   ");
		return s.replaceAll("(\\r|\\n|\\r\\n)+", " ");
	}

	private static class CopyAction extends Action {

		private final Table table;
		private final Converter converter;
		private final boolean selectedOnly;

		CopyAction(Table table, Converter converter, boolean selectedOnly) {
			super();
			this.table = table;
			this.converter = converter;
			this.selectedOnly = selectedOnly;
			setImageDescriptor(Icon.COPY.descriptor());
			var text = selectedOnly
					? "Copy selection"
					: "Copy table";
			setText(text);
			setToolTipText(text);

			// listen to CTRL+C when the selectedOnly flag is set
			// with this it is safe to bind a `copy-all` and
			// `copy-selected` action to the same table
			if (selectedOnly) {
				table.addListener(SWT.KeyUp, e -> {
					if (((e.stateMask & SWT.CTRL) == SWT.CTRL)
							&& (e.keyCode == 'c' || e.keyCode == 'C')) {
						run();
					}
				});
			}
		}

		@Override
		public void run() {
			if (table == null || table.isDisposed())
				return;
			var text = new StringBuilder();
			int columns = table.getColumnCount();

			// copy headers
			for (int col = 0; col < columns; col++) {
				if (col > 0) {
					text.append('\t');
				}
				var header = table.getColumn(col).getText();
				text.append(header);
			}
			text.append('\n');

			// copy items
			var items = selectedOnly
					? table.getSelection()
					: table.getItems();
			for (var item : items) {
				for (int col = 0; col < columns; col++) {
					if (col > 0) {
						text.append('\t');
					}
					text.append(converter.asString(item, col));
				}
				text.append('\n');
			}

			// fill the clipboard
			var clipboard = new Clipboard(UI.shell().getDisplay());
			clipboard.setContents(
					new String[] { text.toString() },
					new Transfer[] { TextTransfer.getInstance() });
			clipboard.dispose();
		}
	}
}
