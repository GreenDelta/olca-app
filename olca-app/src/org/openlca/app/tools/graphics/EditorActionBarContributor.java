package org.openlca.app.tools.graphics;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IActionBars2;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.openlca.app.navigation.Navigator;
import org.slf4j.LoggerFactory;

public abstract class EditorActionBarContributor extends
		MultiPageEditorActionBarContributor {

	private IActionBars2 actionBars2;
	private MultiPageSubActionBars graphicalSubActionBars;
	private MultiPageSubActionBars activeEditorActionBars;

	@Override
	public void init(IActionBars bars) {
		super.init(bars);
		assert bars instanceof IActionBars2;
		actionBars2 = (IActionBars2) bars;
	}

	@Override
	public void setActivePage(IEditorPart activePage) {
		setActiveActionBars(null, activePage);
		if (activePage instanceof BasicGraphicalEditor)
			setActiveActionBars(getGraphicalSubActionBars(), activePage);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (graphicalSubActionBars != null) {
			graphicalSubActionBars.dispose();
			graphicalSubActionBars = null;
		}
	}

	/**
	 * Switches the active action bars.
	 */
	private void setActiveActionBars(MultiPageSubActionBars actionBars,
																	 IEditorPart activeEditor) {
		if (activeEditorActionBars != null
				&& activeEditorActionBars != actionBars) {
			activeEditorActionBars.deactivate();
		}
		activeEditorActionBars = actionBars;
		if (activeEditorActionBars != null) {
			activeEditorActionBars.setEditorPart(activeEditor);
			activeEditorActionBars.activate();
		}
	}

	/**
	 * @return Sets the bar manager for the graphical editor.
	 */
	public void setGraphicalSubActionBars() {
		if (getPage() != null && actionBars2 != null)
			graphicalSubActionBars = getNewSubActionBars();
	}

	/**
	 * @return Returns the bar manager for the graphical editor.
	 */
	public MultiPageSubActionBars getGraphicalSubActionBars() {
		if (graphicalSubActionBars == null)
			setGraphicalSubActionBars();
		return graphicalSubActionBars;
	}

	public IActionBars2 getActionBars2() {
		return actionBars2;
	}

	public abstract MultiPageSubActionBars getNewSubActionBars();

	/**
	 * This method fixes two problems:
	 *  1. sub-actions were disabled when opening the first model graph of the
	 *  session.
	 *  2. sub-actions in the toolbar were invisible after closing every Product
	 *  System editors and opening a new one.
	 * <p>
	 *  To solve the first issue, the focus is set to the Navigator and back to
	 *  the model graph editor. This operation somehow updates the actions and
	 *  makes them enabled.
	 *  The second issue is solved by locking and unlocking the toolbar (or the
	 *  inverse as it depends on the current status of the toolbar). The first
	 *  execution of the action makes the action visible. The second execution
	 *  reset the toolbar to its initial state.
	 */
	public static void refreshActionBar(MultiPageEditorPart part) {
		var log = LoggerFactory.getLogger(EditorActionBarContributor.class);
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.showView(Navigator.ID);
			part.setFocus();
		} catch (PartInitException ex) {
			log.error("Error when focusing on the graphical editor.", ex);
		}

		var service = PlatformUI.getWorkbench().getService(IHandlerService.class);
		String commandId = IWorkbenchCommandConstants.WINDOW_LOCK_TOOLBAR;
		try {
			service.executeCommand(commandId, null);
			service.executeCommand(commandId, null);
		} catch (ExecutionException | NotDefinedException | NotEnabledException |
						 NotHandledException e) {
			log.error("Error when (un)locking the tool bar.", e);
		}

	}

}
