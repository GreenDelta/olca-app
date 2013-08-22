package org.openlca.app.util;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageManager;
import org.openlca.app.resources.ImageType;

/**
 * Factory methods for some standard actions, ready for Java 8, e.g.:
 * 
 * <code> Actions.createAdd(() -> aBlock); </code>
 * 
 */
public class Actions {

	public static final Action createAdd(final Runnable runnable) {
		Action action = new Action() {
			{
				setText(Messages.AddAction_Text);
				setImageDescriptor(ImageManager
						.getImageDescriptor(ImageType.ADD_ICON));
				setDisabledImageDescriptor(ImageManager
						.getImageDescriptor(ImageType.ADD_ICON_DISABLED));
			}

			@Override
			public void run() {
				runnable.run();
			}
		};
		return action;
	}

	public static final Action createRemove(final Runnable runnable) {
		Action action = new Action() {
			{
				setText(Messages.RemoveAction_Text);
				setImageDescriptor(ImageManager
						.getImageDescriptor(ImageType.DELETE_ICON));
				setDisabledImageDescriptor(ImageManager
						.getImageDescriptor(ImageType.DELETE_ICON_DISABLED));
			}

			@Override
			public void run() {
				runnable.run();
			}
		};
		return action;
	}

	/** Creates a context menu with the given actions on the table viewer. */
	public static void bind(TableViewer viewer, Action... actions) {
		Table table = viewer.getTable();
		if (table == null)
			return;
		MenuManager menu = new MenuManager();
		for (Action action : actions)
			menu.add(action);
		table.setMenu(menu.createContextMenu(table));
	}

	/** Creates buttons for the given actions in a section tool-bar. */
	public static void bind(Section section, Action... actions) {
		ToolBarManager toolBar = new ToolBarManager();
		for (Action action : actions)
			toolBar.add(action);
		ToolBar control = toolBar.createControl(section);
		section.setTextClient(control);
	}

}
