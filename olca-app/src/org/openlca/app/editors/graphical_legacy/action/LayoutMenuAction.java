package org.openlca.app.editors.graphical_legacy.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.editors.graphical_legacy.command.LayoutCommand;
import org.openlca.app.editors.graphical_legacy.layout.LayoutType;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;

public class LayoutMenuAction extends Action {

	private final LayoutAction minimalAction;
	private final LayoutAction treeAction;

	public LayoutMenuAction(GraphEditor editor) {
		minimalAction = new LayoutAction(editor, LayoutType.MINIMAL_TREE_LAYOUT);
		treeAction = new LayoutAction(editor, LayoutType.TREE_LAYOUT);
		setId(ActionIds.LAYOUT_MENU);
		setText(M.Layout);
		setImageDescriptor(Icon.LAYOUT.descriptor());
		setMenuCreator(new MenuCreator());
	}

	private class MenuCreator implements IMenuCreator {

		@Override
		public void dispose() {
		}

		@Override
		public Menu getMenu(Control control) {
			var menu = fill(new Menu(control));
			control.setMenu(menu);
			return menu;
		}

		@Override
		public Menu getMenu(Menu parent) {
			return fill(new Menu(parent));
		}

		private Menu fill(Menu menu) {
			var treeItem = new MenuItem(menu, SWT.RADIO);
			treeItem.setText(treeAction.getText());
			Controls.onSelect(treeItem, $ -> treeAction.run());
			treeItem.setSelection(true);
			var minimalItem = new MenuItem(menu, SWT.RADIO);
			minimalItem.setText(minimalAction.getText());
			Controls.onSelect(treeItem, $ -> minimalAction.run());
			return menu;
		}
	}

	private static class LayoutAction extends Action {

		private final GraphEditor editor;
		private final LayoutType layoutType;

		LayoutAction(GraphEditor editor, LayoutType layoutType) {
			this.editor = editor;
			setText(NLS.bind(M.LayoutAs, layoutType.getDisplayName()));
			switch (layoutType) {
				case TREE_LAYOUT -> setId(ActionIds.LAYOUT_TREE);
				case MINIMAL_TREE_LAYOUT -> setId(ActionIds.LAYOUT_MINIMAL_TREE);
			}
			this.layoutType = layoutType;
		}

		@Override
		public void run() {
			var command = new LayoutCommand(editor, layoutType);
			editor.getCommandStack().execute(command);
		}
	}
}
