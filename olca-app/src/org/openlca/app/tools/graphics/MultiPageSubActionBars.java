package org.openlca.app.tools.graphics;

import java.util.Objects;

import org.eclipse.jface.action.ContributionManager;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.SubCoolBarManager;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IActionBars2;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.SubActionBars2;


/**
 * An extended subcool bar manager for used with the
 * <code>MultiPageEditorPart</code>.
 */
public class MultiPageSubActionBars extends SubActionBars2 {

	private IEditorActionBarContributor contributor;
	private final String type;
	private IToolBarManager toolBarManager;
	private ToolBarContributionItem toolBarContributionItem;
	private PartListener partListener;

	/**
	 * Default constructor.
	 */
	public MultiPageSubActionBars(IWorkbenchPage page, IActionBars2 parent,
																IEditorActionBarContributor subContributor, String type) {
		super(parent, parent.getServiceLocator());
		this.type = type;
		partListener = new PartListener(page);
		contributor = subContributor;
		contributor.init(this, page);
	}

	/**
	 * @return the action bar contributor
	 */
	public IEditorActionBarContributor getContributor() {
		return contributor;
	}

	/**
	 * Changes the active editor part.
	 */
	public void setEditorPart(IEditorPart editorPart) {
		contributor.setActiveEditor(editorPart);
	}

	@Override
	public IToolBarManager getToolBarManager() {
		if (toolBarManager == null) {
			var parentCoolBarManager = getTopCoolBarManager();
			if (parentCoolBarManager == null)
				return null;
			var foundItem = parentCoolBarManager.find(type);
			if (foundItem instanceof ToolBarContributionItem item
					&& item.getToolBarManager() != null) {
				toolBarContributionItem = item;
				toolBarManager = toolBarContributionItem.getToolBarManager();
			} else {
				if (parentCoolBarManager instanceof ContributionManager manager) {
					toolBarManager = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
					toolBarContributionItem = new ToolBarContributionItem(
							toolBarManager, type);
					if (!manager.replaceItem(type, toolBarContributionItem))
						manager.add(toolBarContributionItem);
				}
				else return null;
				}
			toolBarContributionItem.setVisible(getActive());
			toolBarManager.markDirty();
		}
		return toolBarManager;
	}

	/**
	 * @return the top-level cool bar manager instance
	 */
	private ICoolBarManager getTopCoolBarManager() {
		var coolBarManager = getCastedParent().getCoolBarManager();
		while (coolBarManager instanceof SubCoolBarManager m
				&& m.getParent() instanceof ICoolBarManager manager) {
			coolBarManager = manager;
		}
		return coolBarManager;
	}

	@Override
	public void dispose() {
		super.dispose();
		contributor.dispose();
		contributor = null;

		if (toolBarContributionItem != null) {
			toolBarContributionItem.dispose();
			toolBarContributionItem = null;
		}

		if (toolBarManager != null) {
			toolBarManager.removeAll();
			toolBarManager = null;
		}

		partListener.dispose();
		partListener = null;
	}

	@Override
	protected void setActive(boolean value) {
		if (getActive() == value) {
			return;
		}
		super.setActive(value);

		var parentCoolBarManager = getTopCoolBarManager();
		if (parentCoolBarManager != null)
			parentCoolBarManager.markDirty();

		if (toolBarManager != null && parentCoolBarManager != null) {
			var items = toolBarManager.getItems();

			for (var item : items)
				item.setVisible(value);

			toolBarManager.markDirty();
			toolBarManager.update(false);
		}

		if (value) {
			var globals = getGlobalActionHandlers();
			for (var nextEntry : globals.entrySet())
				getParent().setGlobalActionHandler(nextEntry.getKey(),
						nextEntry.getValue());
		} else
			getParent().clearGlobalActionHandlers();

		getParent().updateActionBars();
	}

	/**
	 * Inner class to be able to be notified when parts are activated.
	 */
	private class PartListener implements IPartListener {

		private IWorkbenchPage page;

		/**
		 * Default constructor.
		 */
		public PartListener(IWorkbenchPage page) {
			this.page = page;
			this.page.addPartListener(this);
		}

		/**
		 * Default cleanup method.
		 */
		public void dispose() {
			page.removePartListener(this);
			page = null;
		}

		@Override
		public void partActivated(IWorkbenchPart part) {
			if (part instanceof IEditorPart editor) {
				if (!Objects.equals(editor.getEditorSite().getActionBars(), getParent())
						&& getActive()) {
					deactivate();
					updateActionBars();
				}
			}
		}

		@Override
		public void partBroughtToTop(IWorkbenchPart part) {}

		@Override
		public void partClosed(IWorkbenchPart part) {}

		@Override
		public void partDeactivated(IWorkbenchPart part) {}

		@Override
		public void partOpened(IWorkbenchPart part) {}

	}

}
