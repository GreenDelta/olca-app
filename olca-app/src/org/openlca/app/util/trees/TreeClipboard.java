package org.openlca.app.util.trees;

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
import org.openlca.app.M;
import org.openlca.app.util.Actions;
import org.openlca.app.rcp.images.Images;
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
	public static Action onCopy(Tree tree) {
		tree.addListener(SWT.KeyUp, (event) -> {
			if (event.stateMask == SWT.CTRL && event.keyCode == 'c') {
				copy(tree);
			}
		});
		ImageDescriptor image = Images.platformDescriptor(ISharedImages.IMG_TOOL_COPY);
		return Actions.create(M.Copy, image, () -> copy(tree));
	}

	private static void copy(Tree tree) {
		if (tree == null)
			return;
		StringBuilder text = new StringBuilder();
		copyHeaders(tree, text);
		copyItems(tree, text);
		Clipboard clipboard = new Clipboard(UI.shell().getDisplay());
		clipboard.setContents(new String[] { text.toString() },
				new Transfer[] { TextTransfer.getInstance() });
		clipboard.dispose();
	}

	private static void copyHeaders(Tree tree, StringBuilder text) {
		int cols = tree.getColumnCount();
		for (int col = 0; col < cols; col++) {
			TreeColumn column = tree.getColumn(col);
			String s = column.getText();
			text.append(s == null ? "" : s);
			if (col != (cols - 1))
				text.append('\t');
		}
		text.append('\n');
	}

	private static void copyItems(Tree tree, StringBuilder text) {
		int cols = tree.getColumnCount();
		for (TreeItem item : tree.getSelection()) {
			for (int col = 0; col < cols; col++) {
				String s = item.getText(col);
				text.append(s == null ? "" : s);
				if (col != (cols - 1))
					text.append('\t');
			}
			text.append('\n');
		}
	}
}
