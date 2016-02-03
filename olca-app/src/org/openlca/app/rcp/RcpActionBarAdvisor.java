package org.openlca.app.rcp;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.openlca.app.Config;
import org.openlca.app.M;
import org.openlca.app.devtools.js.JavaScriptEditor;
import org.openlca.app.devtools.python.PythonEditor;
import org.openlca.app.devtools.sql.SqlEditor;
import org.openlca.app.editors.LogFileEditor;
import org.openlca.app.editors.StartPage;
import org.openlca.app.rcp.browser.MozillaConfigView;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.plugins.PluginManager;
import org.openlca.app.util.Actions;
import org.openlca.app.util.DefaultInput;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.Editors;

public class RcpActionBarAdvisor extends ActionBarAdvisor {

	private IWorkbenchAction aboutAction;
	private IWorkbenchAction closeAction;
	private IWorkbenchAction closeAllAction;
	private IWorkbenchAction exitAction;
	private IWorkbenchAction exportAction;
	private IWorkbenchAction importAction;
	private IWorkbenchAction preferencesAction;
	private IWorkbenchAction saveAction;
	private IWorkbenchAction saveAllAction;
	private IWorkbenchAction saveAsAction;
	private IContributionItem showViews;
	private IContributionItem showPerspectives;

	public RcpActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	@Override
	protected void fillCoolBar(ICoolBarManager coolBar) {
		IToolBarManager toolbar = new ToolBarManager(SWT.FLAT | SWT.LEFT);
		coolBar.add(new ToolBarContributionItem(toolbar, "main"));
		toolbar.add(new HomeAction());
		toolbar.add(saveAction);
		toolbar.add(saveAsAction);
		toolbar.add(saveAllAction);
		coolBar.add(toolbar);
	}

	@Override
	protected void fillMenuBar(IMenuManager menuBar) {
		super.fillMenuBar(menuBar);
		fillFileMenu(menuBar);
		fillWindowMenu(menuBar);
		fillHelpMenu(menuBar);
	}

	private void fillHelpMenu(IMenuManager menuBar) {
		MenuManager helpMenu = new MenuManager(M.Help, IWorkbenchActionConstants.M_HELP);
		HelpAction helpAction = new HelpAction();
		helpMenu.add(helpAction);
		helpMenu.add(new Separator());
		helpMenu.add(new OpenLogAction());
		helpMenu.add(aboutAction);
		menuBar.add(helpMenu);
	}

	private void fillFileMenu(IMenuManager menuBar) {
		MenuManager menu = new MenuManager(M.File, IWorkbenchActionConstants.M_FILE);
		menu.add(saveAction);
		menu.add(saveAsAction);
		menu.add(saveAllAction);
		menu.add(new Separator());
		menu.add(closeAction);
		menu.add(closeAllAction);
		menu.add(new Separator());
		menu.add(preferencesAction);
		menu.add(new OpenPluginManagerAction());
		menu.add(new Separator());
		menu.add(importAction);
		menu.add(exportAction);
		menu.add(new Separator());
		menu.add(exitAction);
		menuBar.add(menu);
	}

	private void fillWindowMenu(IMenuManager menuBar) {
		MenuManager windowMenu = new MenuManager(M.Window, IWorkbenchActionConstants.M_WINDOW);
		MenuManager viewMenu = new MenuManager(M.Showviews);
		viewMenu.add(showViews);
		windowMenu.add(viewMenu);
		MenuManager perspectivesMenu = new MenuManager(M.Showperspectives);
		perspectivesMenu.add(showPerspectives);
		windowMenu.add(perspectivesMenu);
		createDeveloperMenu(windowMenu);
		windowMenu.add(new FormulaConsoleAction());
		if (MozillaConfigView.canShow()) {
			windowMenu.add(Actions.create(M.BrowserConfiguration, Icon.FIREFOX.descriptor(),
					MozillaConfigView::open));
		}
		menuBar.add(windowMenu);
	}

	private void createDeveloperMenu(MenuManager windowMenu) {
		windowMenu.add(new Separator());
		MenuManager devMenu = new MenuManager(M.DeveloperTools);
		windowMenu.add(devMenu);
		devMenu.add(Actions.create("SQL", Icon.SQL.descriptor(), SqlEditor::open));
		devMenu.add(Actions.create("JavaScript", Icon.JAVASCRIPT.descriptor(), JavaScriptEditor::open));
		devMenu.add(Actions.create("Python", Icon.PYTHON.descriptor(), PythonEditor::open));
		windowMenu.add(new Separator());
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
		showViews = ContributionItemFactory.VIEWS_SHORTLIST.create(window);
		showPerspectives = ContributionItemFactory.PERSPECTIVES_SHORTLIST.create(window);
		aboutAction = ActionFactory.ABOUT.create(window);
	}

	private class HelpAction extends Action {
		public HelpAction() {
			setText(M.OnlineHelp);
			setToolTipText(M.OnlineHelp);
			setImageDescriptor(Icon.HELP.descriptor());
		}

		@Override
		public void run() {
			Desktop.browse(Config.HELP_URL);
		}
	}

	private class HomeAction extends Action {
		public HomeAction() {
			setImageDescriptor(Icon.HOME.descriptor());
			setText(M.Home);
		}

		@Override
		public void run() {
			StartPage.open();
		}
	}

	private class OpenPluginManagerAction extends Action {
		public OpenPluginManagerAction() {
			setText(M.ManagePlugins);
			setToolTipText(M.OpenPluginManager);
		}

		@Override
		public void run() {
			new PluginManager().open();
		}
	}

	private class OpenLogAction extends Action {
		public OpenLogAction() {
			setText(M.OpenLogFile);
			setToolTipText("Opens the openLCA log file");
		}

		@Override
		public void run() {
			Editors.open(new DefaultInput(LogFileEditor.ID), LogFileEditor.ID);
		}
	}
}
