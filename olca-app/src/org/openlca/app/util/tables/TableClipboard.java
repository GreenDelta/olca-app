package org.openlca.app.util.tables;

import java.util.function.Consumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;

public final class TableClipboard {

	private TableClipboard() {
	}

	/**
	 * Registers Ctr+c for copying table content to clipboard and returns an
	 * action which also calls this function.
	 */
	public static Action onCopy(TableViewer viewer) {
		return onCopy(viewer.getTable(), TableClipboard::text);
	}

	/**
	 * Registers Ctr+c for copying table content to clipboard and returns an
	 * action which also calls this function. The given converter function is
	 * used to transform the respective table columns into string
	 * representations.
	 */
	public static Action onCopy(TableViewer viewer, Converter converter) {
		return onCopy(viewer.getTable(), converter);
	}

	/**
	 * Registers Ctr+v for pasting table content from clipboard and returns an
	 * action which also calls this function.
	 */
	public static Action onPaste(TableViewer viewer, Consumer<String> fn) {
		return onPaste(viewer.getTable(), fn);
	}

	private static Action onCopy(Table table, Converter converter) {
		table.addListener(SWT.KeyUp, e -> {
			if (((e.stateMask & SWT.CTRL) == SWT.CTRL)
					&& (e.keyCode == 'c' || e.keyCode == 'C')) {
				copy(table, converter);
			}
		});
		ImageDescriptor image = Icon.COPY.descriptor();
		return Actions.create(M.Copy, image, () -> copy(table, converter));
	}

	private static Action onPaste(Table table, Consumer<String> fn) {
		table.addListener(SWT.KeyUp, e -> {
			if (((e.stateMask & SWT.CTRL) == SWT.CTRL)
					&& (e.keyCode == 'v' || e.keyCode == 'V')) {
				paste(table, fn);
			}
		});
		ImageDescriptor image = Icon.PASTE.descriptor();
		return Actions.create(M.Paste, image, () -> paste(table, fn));
	}

	private static void copy(Table table, Converter converter) {
		if (table == null)
			return;
		StringBuilder text = new StringBuilder();
		copyHeaders(table, text);
		copyItems(table, converter, text);
		Clipboard clipboard = new Clipboard(UI.shell().getDisplay());
		clipboard.setContents(new String[] { text.toString() },
				new Transfer[] { TextTransfer.getInstance() });
		clipboard.dispose();
	}

	private static void copyHeaders(Table table, StringBuilder text) {
		int cols = table.getColumnCount();
		for (int col = 0; col < cols; col++) {
			TableColumn column = table.getColumn(col);
			String s = column.getText();
			text.append(s == null ? "" : s);
			if (col != (cols - 1))
				text.append('\t');
		}
		text.append('\n');
	}

	private static void copyItems(Table table, Converter converter,
			StringBuilder buffer) {
		int cols = table.getColumnCount();
		for (TableItem item : table.getSelection()) {
			for (int col = 0; col < cols; col++) {
				String s = converter.asString(item, col);
				buffer.append(s);
				if (col != (cols - 1)) {
					buffer.append('\t');
				}
			}
			buffer.append('\n');
		}
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
	 * A converter function returns the string presentation of a given column
	 * for a table item. This function can be used to write specific converter
	 * functions for table clipboards.
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
}
