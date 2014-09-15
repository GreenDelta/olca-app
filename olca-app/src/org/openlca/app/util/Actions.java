package org.openlca.app.util;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageManager;
import org.openlca.app.rcp.ImageType;

/**
 * Factory methods for some standard actions, ready for Java 8, e.g.:
 * <p/>
 * <code> Actions.onAdd(() -> aBlock); </code>
 */
public class Actions {

	private Actions() {
	}

	public static Action create(final String title,
			final ImageDescriptor image, final Runnable runnable) {
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

	public static Action onAdd(final Runnable runnable) {
		return new Action() {
			{
				setText(Messages.CreateNew);
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
	}

	public static Action onCalculate(final Runnable runnable) {
		return new Action() {
			{
				setText(Messages.CalculateResults);
				setImageDescriptor(ImageType.CALCULATE_ICON.getDescriptor());
			}

			@Override
			public void run() {
				runnable.run();
			}
		};
	}

	public static Action onRemove(final Runnable runnable) {
		return new Action() {
			{
				setText(Messages.RemoveSelected);
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
	}

	public static Action onSave(final Runnable runnable) {
		return new Action() {
			{
				setText(Messages.Save);
				setToolTipText(Messages.Save);
				ImageDescriptor image = ImageType
						.getPlatformDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT);
				setImageDescriptor(image);
				ImageDescriptor imageDis = ImageType
						.getPlatformDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT_DISABLED);
				setDisabledImageDescriptor(imageDis);
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
	 * Creates buttons for the given actions in a section tool-bar.
	 */
	public static void bind(Section section, Action... actions) {
		ToolBarManager toolBar = new ToolBarManager();
		for (Action action : actions)
			toolBar.add(action);
		ToolBar control = toolBar.createControl(section);
		section.setTextClient(control);
	}

}
