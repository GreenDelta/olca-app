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
	public static Action onCopy(TreeViewer viewer, Provider label) {
		return onCopy(viewer.getTree(), label);
	}

	/**
	 * Registers Ctr+c for copying table content to clipboard and returns an
	 * action which also calls this function.
	 */
	public static Action onCopy(Tree tree) {
		return onCopy(tree, new DefaultProvider(tree));
	}

	/**
	 * Registers Ctr+c for copying table content to clipboard and returns an
	 * action which also calls this function.
	 */
	public static Action onCopy(Tree tree, Provider label) {
		tree.addListener(SWT.KeyUp, (event) -> {
			if (event.stateMask == SWT.CTRL && event.keyCode == 'c') {
				copy(tree, label);
			}
		});
		ImageDescriptor image = Icon.COPY.descriptor();
		return Actions.create(M.Copy, image, () -> copy(tree, label));
	}

	private static void copy(Tree tree, Provider label) {
		if (tree == null)
			return;
		var text = new StringBuilder();
		copyHeaders(label, text);
		copyItems(tree, label, text);
		var clipboard = new Clipboard(UI.shell().getDisplay());
		clipboard.setContents(
				new String[]{text.toString()},
				new Transfer[]{TextTransfer.getInstance()});
		clipboard.dispose();
	}

	private static void copyHeaders(Provider provider, StringBuilder text) {
		int cols = provider.columns();
		for (int col = 0; col < cols; col++) {
			if (col > 0) {
				text.append('\t');
			}
			var s = provider.getHeader(col);
			text.append(s == null ? "" : s);
		}
		text.append('\n');
	}

	private static void copyItems(
			Tree tree, Provider provider, StringBuilder text) {
		for (var item : tree.getSelection()) {
			for (int col = 0; col < provider.columns(); col++) {
				if (col > 0) {
					text.append('\t');
				} else {
					int level = levelOf(item);
					if (level > 0) {
						text.append("  ".repeat(level));
					}
				}
				var s = provider.getLabel(item, col);
				text.append(s == null ? "" : s);
			}
			text.append('\n');
		}
	}

	private static int levelOf(TreeItem item) {
		if (item == null)
			return -1;
		int level = 0;
		var parent = item.getParentItem();
		while (parent != null) {
			level++;
			parent = parent.getParentItem();
		}
		return level;
	}

	public interface Provider {

		int columns();

		String getHeader(int col);

		String getLabel(TreeItem item, int col);

	}

	private record DefaultProvider(Tree tree) implements Provider {

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
