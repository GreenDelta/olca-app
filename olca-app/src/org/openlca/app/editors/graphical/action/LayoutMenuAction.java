package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.layout.LayoutType;
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

		private void createMenu(Menu menu) {
			var treeItem = new MenuItem(menu, SWT.RADIO);
			treeItem.setText(treeAction.getText());
			Controls.onSelect(treeItem, (e) -> treeAction.run());
			treeItem.setSelection(true);

			var minimalItem = new MenuItem(menu, SWT.RADIO);
			minimalItem.setText(minimalAction.getText());
			Controls.onSelect(treeItem, (e) -> minimalAction.run());
		}

		@Override
		public void dispose() {
		}

		@Override
		public Menu getMenu(Control control) {
			var menu = new Menu(control);
			createMenu(menu);
			control.setMenu(menu);
			return menu;
		}

		@Override
		public Menu getMenu(Menu parent) {
			var menu = new Menu(parent);
			createMenu(menu);
			return menu;
		}
	}
}
