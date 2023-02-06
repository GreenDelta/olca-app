package org.openlca.app.util;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;

public class Actions {

	private Actions() {
	}

	public static Action create(String title, Runnable fn) {
		return create(title, null, fn);
	}

	public static Action create(String title,
			ImageDescriptor image, Runnable runnable) {
		return new Action() {
			{
				setText(title);
				setToolTipText(title);
				setImageDescriptor(image);
			}

			@Override
			public void run() {
				runnable.run();
			}
		};
	}

	public static Action onAdd(Runnable runnable) {
		return new Action() {
			{
				setText(M.CreateNew);
				setImageDescriptor(Icon.ADD.descriptor());
				setDisabledImageDescriptor(Icon.ADD_DISABLED.descriptor());
			}

			@Override
			public void run() {
				runnable.run();
			}
		};
	}

	public static Action onEdit(Runnable fn) {
		return create(M.Edit, Icon.EDIT.descriptor(), fn);
	}

	public static Action onCalculate(Runnable fn) {
		return create(M.CalculateResults, Icon.RUN.descriptor(), fn);
	}

	public static Action onOpen(Runnable fn) {
		return create(M.Open, Icon.FOLDER_OPEN.descriptor(), fn);
	}

	public static Action onRemove(Runnable runnable) {
		return new Action() {
			{
				setText(M.RemoveSelected);
				setImageDescriptor(
						Icon.DELETE.descriptor());
				setDisabledImageDescriptor(
						Icon.DELETE_DISABLED.descriptor());
			}

			@Override
			public void run() {
				runnable.run();
			}
		};
	}

	public static Action onSave(Runnable runnable) {
		return new Action() {
			{
				setText(M.Save);
				setToolTipText(M.Save);
				setImageDescriptor(
						Icon.SAVE.descriptor());
				setDisabledImageDescriptor(
						Icon.SAVE_DISABLED.descriptor());
			}

			@Override
			public void run() {
				runnable.run();
			}
		};
	}

	/**
	 * Creates a context menu with the given actions on the table viewer.
	 */
	public static void bind(TableViewer viewer, Action... actions) {
		Table table = viewer.getTable();
		if (table == null)
			return;
		MenuManager menu = new MenuManager();
		for (Action action : actions)
			menu.add(action);
		table.setMenu(menu.createContextMenu(table));
	}

	/**
	 * Creates a context menu with the given actions on the tree viewer.
	 */
	public static void bind(TreeViewer viewer, Action... actions) {
		var tree = viewer.getTree();
		if (tree == null)
			return;
		var menu = new MenuManager();
		for (var action : actions) {
			menu.add(action);
		}
		tree.setMenu(menu.createContextMenu(tree));
	}

	/**
	 * Creates buttons for the given actions in a section tool-bar.
	 */
	public static void bind(Section section, Action... actions) {
		var toolBar = new ToolBarManager();
		for (var action : actions) {
			toolBar.add(action);
		}
		var control = toolBar.createControl(section);
		// we need to set the background color of the toolbar
		// here, otherwise it is grey on Windows; not sure if
		// this works with dark mode though
		control.setBackground(section.getBackground());
		section.setTextClient(control);
	}

}
