package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.editors.graphical.layout.GraphLayoutType;
import org.openlca.app.resources.ImageType;

class LayoutMenuAction extends Action {

	private ProductSystemGraphEditor editor;
	private LayoutAction minimalLayoutAction = new LayoutAction(
			GraphLayoutType.MINIMAL_TREE_LAYOUT);
	private LayoutAction treeLayoutAction = new LayoutAction(
			GraphLayoutType.TREE_LAYOUT);

	LayoutMenuAction() {
		setId(ActionIds.LAYOUT_MENU);
		setText(Messages.Systems_AppActionBarContributorClass_LayoutActionText);
		setImageDescriptor(ImageType.LAYOUT_ICON.getDescriptor());
		setMenuCreator(new MenuCreator());
	}

	void setEditor(ProductSystemGraphEditor editor) {
		minimalLayoutAction.setModel(editor.getModel());
		treeLayoutAction.setModel(editor.getModel());
		this.editor = editor;
	}

	private class MenuCreator implements IMenuCreator {

		private Menu createMenu(Menu menu) {
			MenuItem minimalItem = new MenuItem(menu, SWT.NONE);
			minimalItem.setText(minimalLayoutAction.getText());
			minimalItem.setImage(minimalLayoutAction.getImageDescriptor()
					.createImage());
			minimalItem.addSelectionListener(new LayoutListener(
					minimalLayoutAction));

			MenuItem treeItem = new MenuItem(menu, SWT.NONE);
			treeItem.setText(treeLayoutAction.getText());
			treeItem.setImage(treeLayoutAction.getImageDescriptor()
					.createImage());
			treeItem.addSelectionListener(new LayoutListener(treeLayoutAction));

			final MenuItem routedCheck = new MenuItem(menu, SWT.CHECK);
			routedCheck.setText(Messages.Systems_Route);
			routedCheck.setSelection(editor.isRouted());
			routedCheck.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {

				}

				@Override
				public void widgetSelected(SelectionEvent e) {
					editor.setRouted(routedCheck.getSelection());
				}
			});
			return menu;
		}

		@Override
		public void dispose() {
			// nothing to dispose
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

	private class LayoutListener extends SelectionAdapter {

		private LayoutAction action;

		private LayoutListener(LayoutAction action) {
			this.action = action;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			action.run();
		}

	}

}
