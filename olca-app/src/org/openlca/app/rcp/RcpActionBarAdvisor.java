package org.openlca.app.rcp;

import java.util.Objects;

import org.eclipse.core.runtime.IExtension;
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
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.registry.ActionSetRegistry;
import org.eclipse.ui.internal.registry.IActionSetDescriptor;
import org.openlca.app.Config;
import org.openlca.app.M;
import org.openlca.app.components.replace.ReplaceFlowsDialog;
import org.openlca.app.components.replace.ReplaceProvidersDialog;
import org.openlca.app.devtools.ipc.IpcDialog;
import org.openlca.app.devtools.js.JavaScriptEditor;
import org.openlca.app.devtools.python.PythonEditor;
import org.openlca.app.devtools.sql.SqlEditor;
import org.openlca.app.editors.LogFileEditor;
import org.openlca.app.editors.StartPage;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.plugins.PluginManager;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Desktop;
import org.openlca.core.model.ModelType;

@SuppressWarnings("restriction")
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
		DatabaseMenu.addTo(menuBar);
		fillWindowMenu(menuBar);
		fillHelpMenu(menuBar);
		removeActionSets();
	}

	private void removeActionSets() {
		// currently we just remove the cheat-sheets here; see:
		// http://random-eclipse-tips.blogspot.de/2009/02/eclipse-rcp-removing-unwanted_02.html
		ActionSetRegistry reg = WorkbenchPlugin.getDefault().getActionSetRegistry();
		IActionSetDescriptor[] actionSets = reg.getActionSets();
		for (int i = 0; i < actionSets.length; i++) {
			if (Objects.equals(actionSets[i].getId(),
					"org.eclipse.ui.cheatsheets.actionSet")) {
				IExtension ext = actionSets[i].getConfigurationElement()
						.getDeclaringExtension();
				reg.removeExtension(ext, new Object[] { actionSets[i] });
			}
		}
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
		// menu.add(new OpenPluginManagerAction());
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
		windowMenu.add(new Separator());
		createDeveloperMenu(windowMenu);
		createMassReplaceMenu(windowMenu);
		windowMenu.add(new Separator());
		windowMenu.add(new FormulaConsoleAction());
		menuBar.add(windowMenu);
	}

	private void createDeveloperMenu(MenuManager windowMenu) {
		MenuManager devMenu = new MenuManager(M.DeveloperTools);
		windowMenu.add(devMenu);
		devMenu.add(Actions.create("SQL", Icon.SQL.descriptor(), SqlEditor::open));
		devMenu.add(Actions.create("JavaScript", Icon.JAVASCRIPT.descriptor(), JavaScriptEditor::open));
		devMenu.add(Actions.create("Python", Icon.PYTHON.descriptor(), PythonEditor::open));
		devMenu.add(Actions.create("IPC Server", Icon.DATABASE.descriptor(), IpcDialog::show));
	}

	private void createMassReplaceMenu(MenuManager windowMenu) {
		MenuManager m = new MenuManager(M.Bulkreplace);
		windowMenu.add(m);
		m.add(Actions.create(M.Flows, Images.descriptor(ModelType.FLOW), ReplaceFlowsDialog::openDialog));
		m.add(Actions.create(M.Providers, Images.descriptor(ModelType.PROCESS), ReplaceProvidersDialog::openDialog));
	}

	@Override
	protected void makeActions(final IWorkbenchWindow window) {
		// save
		saveAction = ActionFactory.SAVE.create(window);
		saveAction.setImageDescriptor(Icon.SAVE.descriptor());
		saveAction.setDisabledImageDescriptor(Icon.SAVE_DISABLED.descriptor());
		// save as
		saveAsAction = ActionFactory.SAVE_AS.create(window);
		saveAsAction.setImageDescriptor(Icon.SAVE_AS.descriptor());
		saveAsAction.setDisabledImageDescriptor(Icon.SAVE_AS_DISABLED.descriptor());
		// save all
		saveAllAction = ActionFactory.SAVE_ALL.create(window);
		saveAllAction.setImageDescriptor(Icon.SAVE_ALL.descriptor());
		saveAllAction.setDisabledImageDescriptor(Icon.SAVE_ALL_DISABLED.descriptor());
		closeAction = ActionFactory.CLOSE.create(window);
		closeAllAction = ActionFactory.CLOSE_ALL.create(window);
		preferencesAction = ActionFactory.PREFERENCES.create(window);
		preferencesAction.setImageDescriptor(Icon.PREFERENCES.descriptor());
		importAction = ActionFactory.IMPORT.create(window);
		importAction.setImageDescriptor(Icon.IMPORT.descriptor());
		exportAction = ActionFactory.EXPORT.create(window);
		exportAction.setImageDescriptor(Icon.EXPORT.descriptor());
		exitAction = ActionFactory.QUIT.create(window);
		showViews = ContributionItemFactory.VIEWS_SHORTLIST.create(window);
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

	@Deprecated
	private class OpenPluginManagerAction extends Action {
		public OpenPluginManagerAction() {
			setText(M.ManagePlugins);
			setToolTipText(M.OpenPluginManager);
			setImageDescriptor(Icon.MANAGE_PLUGINS.descriptor());
		}

		@Override
		public void run() {
			new PluginManager().open();
		}
	}

	private class OpenLogAction extends Action {
		public OpenLogAction() {
			setText(M.OpenLogFile);
			setImageDescriptor(Icon.FILE.descriptor());
			setToolTipText("Opens the openLCA log file");
		}

		@Override
		public void run() {
			LogFileEditor.open();
		}
	}
}
