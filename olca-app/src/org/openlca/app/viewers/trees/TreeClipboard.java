package org.openlca.app.viewers.trees;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;

public final class TreeClipboard {

	private TreeClipboard() {
	}

	/**
	 * Registers Ctr+c for copying table content to clipboard and returns an
	 * action which also calls this function.
	 */
	public static Action onCopy(TreeViewer viewer) {
		return onCopy(viewer.getTree());
	}

	/**
	 * Registers Ctr+c for copying table content to clipboard and returns an
	 * action which also calls this function.
	 */
	public static Action onCopy(TreeViewer viewer, ClipboardLabelProvider label) {
		return onCopy(viewer.getTree(), label);
	}

	/**
	 * Registers Ctr+c for copying table content to clipboard and returns an
	 * action which also calls this function.
	 */
	public static Action onCopy(Tree tree) {
		return onCopy(tree, new DefaultLabel(tree));
	}

	/**
	 * Registers Ctr+c for copying table content to clipboard and returns an
	 * action which also calls this function.
	 */
	public static Action onCopy(Tree tree, ClipboardLabelProvider label) {
		tree.addListener(SWT.KeyUp, (event) -> {
			if (event.stateMask == SWT.CTRL && event.keyCode == 'c') {
				copy(tree, label);
			}
		});
		ImageDescriptor image = Icon.COPY.descriptor();
		return Actions.create(M.Copy, image, () -> copy(tree, label));
	}

	private static void copy(Tree tree, ClipboardLabelProvider label) {
		if (tree == null)
			return;
		StringBuilder text = new StringBuilder();
		copyHeaders(tree, label, text);
		copyItems(tree, label, text);
		Clipboard clipboard = new Clipboard(UI.shell().getDisplay());
		clipboard.setContents(new String[] { text.toString() }, new Transfer[] { TextTransfer.getInstance() });
		clipboard.dispose();
	}

	private static void copyHeaders(Tree tree, ClipboardLabelProvider label, StringBuilder text) {
		int cols = label.columns();
		for (int col = 0; col < cols; col++) {
			String s = label.getHeader(col);
			text.append(s == null ? "" : s);
			if (col != (cols - 1))
				text.append('\t');
		}
		text.append('\n');
	}

	private static void copyItems(Tree tree, ClipboardLabelProvider label, StringBuilder text) {
		int cols = label.columns();
		for (TreeItem item : tree.getSelection()) {
			for (int col = 0; col < cols; col++) {
				String s = label.getLabel(item, col);
				text.append(s == null ? "" : s);
				if (col != (cols - 1))
					text.append('\t');
			}
			text.append('\n');
		}
	}

	public static interface ClipboardLabelProvider {

		public int columns();

		public String getHeader(int col);

		public String getLabel(TreeItem item, int col);

	}

	private static class DefaultLabel implements ClipboardLabelProvider {

		private Tree tree;

		private DefaultLabel(Tree tree) {
			this.tree = tree;
		}

		@Override
		public int columns() {
			return tree.getColumnCount();
		}

		@Override
		public String getHeader(int col) {
			return tree.getColumn(col).getText();
		}

		@Override
		public String getLabel(TreeItem item, int col) {
			return item.getText(col);
		}

	}

}
