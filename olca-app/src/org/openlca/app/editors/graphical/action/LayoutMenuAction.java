package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.editors.graphical.layout.LayoutType;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;

class LayoutMenuAction extends EditorAction {

	private ProductSystemGraphEditor editor;
	private LayoutAction minimalLayoutAction = new LayoutAction(LayoutType.MINIMAL_TREE_LAYOUT);
	private LayoutAction treeLayoutAction = new LayoutAction(LayoutType.TREE_LAYOUT);

	LayoutMenuAction() {
		setId(ActionIds.LAYOUT_MENU);
		setText(M.Layout);
		setImageDescriptor(Icon.LAYOUT.descriptor());
		setMenuCreator(new MenuCreator());
	}

	@Override
	public void setEditor(ProductSystemGraphEditor editor) {
		minimalLayoutAction.setModel(editor.getModel());
		treeLayoutAction.setModel(editor.getModel());
		this.editor = editor;
	}

	@Override
	protected boolean accept(ISelection selection) {
		return true;
	}
	
	private class MenuCreator implements IMenuCreator {

		private Menu createMenu(Menu menu) {
			MenuItem treeItem = new MenuItem(menu, SWT.RADIO);
			treeItem.setText(treeLayoutAction.getText());
			Controls.onSelect(treeItem, (e) -> treeLayoutAction.run());
			treeItem.setSelection(true);
			MenuItem minimalItem = new MenuItem(menu, SWT.RADIO);
			minimalItem.setText(minimalLayoutAction.getText());
			Controls.onSelect(treeItem, (e) -> minimalLayoutAction.run());
			new MenuItem(menu, SWT.SEPARATOR);
			MenuItem routedCheck = new MenuItem(menu, SWT.CHECK);
			routedCheck.setText(M.Route);
			routedCheck.setSelection(editor.isRouted());
			Controls.onSelect(routedCheck, (e) -> editor.setRouted(routedCheck.getSelection()));
			return menu;
		}

		@Override
		public void dispose() {
		}

		@Override
		public Menu getMenu(Control control) {
			Menu menu = new Menu(control);
			createMenu(menu);
			control.setMenu(menu);
			return menu;
		}

		@Override
		public Menu getMenu(Menu parent) {
			Menu menu = new Menu(parent);
			createMenu(menu);
			return menu;
		}

	}

}
