/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.plugin;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.openlca.app.FormulaConsoleAction;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;

/**
 * The application action bar advisor
 * 
 * @see ActionBarAdvisor
 * @author Sebastian Greve
 * 
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	/**
	 * Action to show the about dialog
	 */
	private IWorkbenchAction aboutAction;

	/**
	 * Action to close an editor
	 */
	private IWorkbenchAction closeAction;

	/**
	 * Action to close all editors
	 */
	private IWorkbenchAction closeAllAction;

	/**
	 * Action to delete an object
	 */
	private RetargetAction deleteAction;

	/**
	 * Action to show the dynamic help
	 */
	private IWorkbenchAction dynamicHelpAction;

	/**
	 * Action to exit the application
	 */
	private IWorkbenchAction exitAction;

	/**
	 * Action to show the export wizards
	 */
	private IWorkbenchAction exportAction;

	/**
	 * Action to open the help contents
	 */
	private IWorkbenchAction helpContentsAction;

	/**
	 * Action to open the help search
	 */
	private IWorkbenchAction helpSearchAction;

	/**
	 * Action to show the import wizards
	 */
	private IWorkbenchAction importAction;

	/**
	 * Action to open a selection in a new editor
	 */
	private IWorkbenchAction newEditorAction;

	/**
	 * Action to open the application in a new window
	 */
	private IWorkbenchAction newWindowAction;

	/**
	 * Action to open an object in an editor
	 */
	private RetargetAction openAction;

	/**
	 * Action to show the preference pages
	 */
	private IWorkbenchAction preferencesAction;

	/**
	 * Action to save an object
	 */
	private IWorkbenchAction saveAction;

	/**
	 * Action to save all objects
	 */
	private IWorkbenchAction saveAllAction;

	/**
	 * Action save an object as a new object
	 */
	private IWorkbenchAction saveAsAction;

	/**
	 * Action to show the available views
	 */
	private IContributionItem showViews;

	private IWorkbenchAction introAction;

	/**
	 * Creates a new instance
	 * 
	 * @param configurer
	 *            The action bar configurer
	 */
	public ApplicationActionBarAdvisor(final IActionBarConfigurer configurer) {
		super(configurer);
	}

	@Override
	protected void fillCoolBar(final ICoolBarManager coolBar) {
		// create save tool bar
		final IToolBarManager saveToolbar = new ToolBarManager(SWT.FLAT
				| SWT.RIGHT);
		saveToolbar.add(saveAction);
		saveToolbar.add(saveAsAction);
		saveToolbar.add(saveAllAction);

		// create open tool bar
		final IToolBarManager openDeleteToolbar = new ToolBarManager(SWT.FLAT
				| SWT.RIGHT);
		openDeleteToolbar.add(openAction);
		openDeleteToolbar.add(deleteAction);

		coolBar.add(saveToolbar);
		coolBar.add(openDeleteToolbar);

		if (introAction != null) {
			// create welcome tool bar
			final IToolBarManager welcomeToolbar = new ToolBarManager(SWT.FLAT
					| SWT.RIGHT);
			welcomeToolbar.add(introAction);
			coolBar.add(welcomeToolbar);
		}
	}

	@Override
	protected void fillMenuBar(final IMenuManager menuBar) {
		super.fillMenuBar(menuBar);
		// create file menu
		final MenuManager menuFile = new MenuManager(Messages.Menu_File,
				IWorkbenchActionConstants.M_FILE);
		menuFile.add(saveAction);
		menuFile.add(saveAsAction);
		menuFile.add(saveAllAction);
		menuFile.add(new Separator());
		menuFile.add(closeAction);
		menuFile.add(closeAllAction);
		menuFile.add(new Separator());
		menuFile.add(preferencesAction);
		menuFile.add(new Separator());
		menuFile.add(importAction);
		menuFile.add(exportAction);
		menuFile.add(new Separator());
		menuFile.add(exitAction);
		menuBar.add(menuFile);

		// create edit menu
		final MenuManager menuEdit = new MenuManager(Messages.Menu_Edit,
				IWorkbenchActionConstants.M_EDIT);
		menuEdit.add(openAction);
		menuEdit.add(deleteAction);
		menuBar.add(menuEdit);

		// create window menu
		final MenuManager menuWindow = new MenuManager(Messages.Menu_Window,
				IWorkbenchActionConstants.M_WINDOW);
		menuWindow.add(newWindowAction);
		menuWindow.add(newEditorAction);

		// create show view sub menu
		final MenuManager viewMenu = new MenuManager(Messages.Menu_ShowViews);
		viewMenu.add(showViews);
		menuWindow.add(viewMenu);
		menuWindow.add(new FormulaConsoleAction());
		menuBar.add(menuWindow);

		// create help menu
		final MenuManager menuHelp = new MenuManager(Messages.Menu_Help,
				IWorkbenchActionConstants.M_HELP);
		if (introAction != null) {
			menuHelp.add(introAction);
		}
		menuHelp.add(helpContentsAction);
		menuHelp.add(helpSearchAction);
		menuHelp.add(dynamicHelpAction);
		menuHelp.add(new Separator());
		menuHelp.add(aboutAction);
		menuBar.add(menuHelp);
	}

	@Override
	protected void makeActions(final IWorkbenchWindow window) {

		saveAction = ActionFactory.SAVE.create(window);

		saveAsAction = ActionFactory.SAVE_AS.create(window);

		saveAllAction = ActionFactory.SAVE_ALL.create(window);

		closeAction = ActionFactory.CLOSE.create(window);

		closeAllAction = ActionFactory.CLOSE_ALL.create(window);

		preferencesAction = ActionFactory.PREFERENCES.create(window);

		importAction = ActionFactory.IMPORT.create(window);

		exportAction = ActionFactory.EXPORT.create(window);

		exitAction = ActionFactory.QUIT.create(window);

		openAction = new RetargetAction("open.action", Messages.Menu_Open);
		openAction.setToolTipText(openAction.getText());
		openAction.setImageDescriptor(ImageType.LOAD_ICON.getDescriptor());
		window.getPartService().addPartListener(openAction);

		deleteAction = new RetargetAction("delete.action", Messages.Menu_Delete);
		deleteAction.setToolTipText(deleteAction.getText());
		deleteAction.setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
		deleteAction.setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
				.getDescriptor());
		window.getPartService().addPartListener(deleteAction);

		newWindowAction = ActionFactory.OPEN_NEW_WINDOW.create(window);

		newEditorAction = ActionFactory.NEW_EDITOR.create(window);

		showViews = ContributionItemFactory.VIEWS_SHORTLIST.create(window);

		helpContentsAction = ActionFactory.HELP_CONTENTS.create(window);

		helpSearchAction = ActionFactory.HELP_SEARCH.create(window);

		dynamicHelpAction = ActionFactory.DYNAMIC_HELP.create(window);

		aboutAction = ActionFactory.ABOUT.create(window);

		// try {
		// introAction = ActionFactory.INTRO.create(window);
		// register(introAction);
		// } catch (NullPointerException npe) {
		// log.debug("Welcome/intro page plugin not found");
		// }
	}
}
