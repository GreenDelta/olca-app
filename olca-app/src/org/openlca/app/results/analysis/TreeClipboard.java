package org.openlca.app.results.analysis;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;

final class TreeClipboard {

	private TreeClipboard() {
	}

	public static Action onCopy(TreeViewer viewer) {
		return onCopy(viewer.getTree());
	}

	public static Action onCopy(Tree tree) {
		tree.addListener(SWT.KeyUp, (event) -> {
			if (event.stateMask == SWT.CTRL && event.keyCode == 'c') {
				copy(tree);
			}
		});
		ImageDescriptor image = ImageType.getPlatformDescriptor(
				ISharedImages.IMG_TOOL_COPY);
		return Actions.create(Messages.Copy, image, () -> copy(tree));
	}

	private static void copy(Tree tree) {
		if (tree == null)
			return;
		StringBuilder text = new StringBuilder();
		int levels = getLevelCount(tree);
		copyHeaders(tree, levels, text);
		copyItems(tree, levels, text);
		Clipboard clipboard = new Clipboard(UI.shell().getDisplay());
		clipboard.setContents(new String[] { text.toString() },
				new Transfer[] { TextTransfer.getInstance() });
		clipboard.dispose();
	}

	private static void copyHeaders(Tree tree, int levels, StringBuilder text) {
		int cols = tree.getColumnCount();
		if (cols < 1)
			return;
		text.append(tree.getColumn(0).getText());
		for (int level = 0; level < levels; level++)
			text.append('\t');
		for (int col = 1; col < cols; col++) {
			TreeColumn column = tree.getColumn(col);
			text.append(column.getText());
			if (col != (cols - 1))
				text.append('\t');
		}
		text.append('\n');
	}

	private static void copyItems(Tree tree, int levels, StringBuilder text) {
		int cols = tree.getColumnCount();
		if (cols < 1)
			return;
		for (TreeItem item : tree.getSelection()) {
			int itemLevel = getLevel(item);
			for (int level = 0; level < levels; level++) {
				if (itemLevel == level)
					text.append(item.getText(0));
				text.append('\t');
			}
			for (int col = 1; col < cols; col++) {
				String s = item.getText(col);
				text.append(s == null ? "" : s);
				if (col != (cols - 1))
					text.append('\t');
			}
			text.append('\n');
		}
	}

	private static int getLevelCount(Tree tree) {
		if (tree == null)
			return -1;
		int count = 0;
		for (TreeItem item : tree.getSelection()) {
			int c = getLevel(item) + 1;
			count = Math.max(c, count);
		}
		return count;
	}

	private static int getLevel(TreeItem item) {
		if (item == null)
			return -1;
		int level = 0;
		TreeItem parent = item.getParentItem();
		while (parent != null) {
			level++;
			parent = parent.getParentItem();
		}
		return level;
	}

}
