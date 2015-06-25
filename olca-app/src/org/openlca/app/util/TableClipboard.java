package org.openlca.app.util;

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
import org.eclipse.ui.ISharedImages;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;

public final class TableClipboard {

	private TableClipboard() {
	}

	/**
	 * Registers Ctr+c for copying table content to clipboard and returns an
	 * action which also calls this function.
	 */
	public static Action onCopy(TableViewer viewer) {
		return onCopy(viewer.getTable());
	}

	/**
	 * Registers Ctr+v for pasting table content from clipboard and returns an
	 * action which also calls this function.
	 */
	public static Action onPaste(TableViewer viewer, Consumer<String> fn) {
		return onPaste(viewer.getTable(), fn);
	}

	/**
	 * Registers Ctr+c for copying table content to clipboard and returns an
	 * action which also calls this function.
	 */
	public static Action onCopy(Table table) {
		table.addListener(SWT.KeyUp, (event) -> {
			if (event.stateMask == SWT.CTRL && event.keyCode == 'c') {
				copy(table);
			}
		});
		ImageDescriptor image = ImageType.getPlatformDescriptor(
				ISharedImages.IMG_TOOL_COPY);
		return Actions.create(Messages.Copy, image, () -> copy(table));
	}

	/**
	 * Registers Ctr+v for pasting table content from clipboard and returns an
	 * action which also calls this function.
	 */
	public static Action onPaste(Table table, Consumer<String> fn) {
		table.addListener(SWT.KeyUp, (event) -> {
			if (event.stateMask == SWT.CTRL && event.keyCode == 'v') {
				paste(table, fn);
			}
		});
		ImageDescriptor image = ImageType.getPlatformDescriptor(
				ISharedImages.IMG_TOOL_PASTE);
		return Actions.create(Messages.Paste, image, () -> paste(table, fn));
	}

	private static void copy(Table table) {
		if (table == null)
			return;
		StringBuilder text = new StringBuilder();
		copyHeaders(table, text);
		copyItems(table, text);
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

	private static void copyItems(Table table, StringBuilder text) {
		int cols = table.getColumnCount();
		for (TableItem item : table.getSelection()) {
			for (int col = 0; col < cols; col++) {
				String s = item.getText(col);
				text.append(s == null ? "" : s);
				if (col != (cols - 1))
					text.append('\t');
			}
			text.append('\n');
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
}
