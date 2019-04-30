package org.openlca.app.rcp;

import java.util.Objects;

import org.eclipse.core.runtime.IExtension;
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
import org.openlca.app.editors.StartPage;
import org.openlca.app.editors.parameters.BigParameterTable;
import org.openlca.app.logging.LogFileEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.tools.mapping.MappingTool;
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
		toolbar.add(Actions.create(
				M.Home, Icon.HOME.descriptor(), StartPage::open));
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
		MenuManager helpMenu = new MenuManager(
				M.Help, IWorkbenchActionConstants.M_HELP);
		helpMenu.add(Actions.create(
				M.OnlineHelp, Icon.HELP.descriptor(),
				() -> Desktop.browse(Config.HELP_URL)));
		helpMenu.add(new Separator());
		helpMenu.add(Actions.create(
				M.OpenLogFile, Icon.FILE.descriptor(), LogFileEditor::open));
		helpMenu.add(aboutAction);
		menuBar.add(helpMenu);
	}

	private void fillFileMenu(IMenuManager menuBar) {
		MenuManager menu = new MenuManager(
				M.File, IWorkbenchActionConstants.M_FILE);
		menu.add(saveAction);
		menu.add(saveAsAction);
		menu.add(saveAllAction);
		menu.add(new Separator());
		menu.add(closeAction);
		menu.add(closeAllAction);
		menu.add(new Separator());
		menu.add(preferencesAction);
		menu.add(new Separator());
		menu.add(importAction);
		menu.add(exportAction);
		menu.add(new Separator());
		menu.add(exitAction);
		menuBar.add(menu);
	}

	private void fillWindowMenu(IMenuManager menuBar) {
		MenuManager menu = new MenuManager(M.Window,
				IWorkbenchActionConstants.M_WINDOW);
		MenuManager viewMenu = new MenuManager(M.Showviews);
		viewMenu.add(showViews);
		menu.add(viewMenu);
		menu.add(new Separator());
		menu.add(Actions.create(
				M.Parameters,
				Images.descriptor(ModelType.PARAMETER),
				BigParameterTable::show));
		createDeveloperMenu(menu);

		// bulk replace
		MenuManager brMenu = new MenuManager(M.Bulkreplace);
		menu.add(brMenu);
		brMenu.add(Actions.create(
				M.Flows, Images.descriptor(ModelType.FLOW),
				ReplaceFlowsDialog::openDialog));
		brMenu.add(Actions.create(
				M.Providers, Images.descriptor(ModelType.PROCESS),
				ReplaceProvidersDialog::openDialog));

		// flow mapping
		MenuManager fmMenu = new MenuManager("Flow mapping (experimental)");
		menu.add(fmMenu);
		fmMenu.add(Actions.create("New", MappingTool::createNew));
		fmMenu.add(Actions.create("Open file", MappingTool::openFile));

		menu.add(new Separator());
		menu.add(new FormulaConsoleAction());
		menuBar.add(menu);
	}

	private void createDeveloperMenu(MenuManager windowMenu) {
		MenuManager devMenu = new MenuManager(M.DeveloperTools);
		windowMenu.add(devMenu);
		devMenu.add(Actions.create("SQL", Icon.SQL.descriptor(), SqlEditor::open));
		devMenu.add(Actions.create("JavaScript", Icon.JAVASCRIPT.descriptor(), JavaScriptEditor::open));
		devMenu.add(Actions.create("Python", Icon.PYTHON.descriptor(), PythonEditor::open));
		devMenu.add(Actions.create("IPC Server", Icon.DATABASE.descriptor(), IpcDialog::show));
	}

	@Override
	protected void makeActions(IWorkbenchWindow window) {
		// save
		saveAction = ActionFactory.SAVE.create(window);
		saveAction.setText(M.Save);
		saveAction.setImageDescriptor(Icon.SAVE.descriptor());
		saveAction.setDisabledImageDescriptor(Icon.SAVE_DISABLED.descriptor());

		// save as
		saveAsAction = ActionFactory.SAVE_AS.create(window);
		saveAsAction.setText(M.SaveAs);
		saveAsAction.setImageDescriptor(Icon.SAVE_AS.descriptor());
		saveAsAction.setDisabledImageDescriptor(Icon.SAVE_AS_DISABLED.descriptor());

		// save all
		saveAllAction = ActionFactory.SAVE_ALL.create(window);
		saveAllAction.setText(M.SaveAll);
		saveAllAction.setImageDescriptor(Icon.SAVE_ALL.descriptor());
		saveAllAction.setDisabledImageDescriptor(Icon.SAVE_ALL_DISABLED.descriptor());

		// close & close all
		closeAction = ActionFactory.CLOSE.create(window);
		closeAction.setText(M.Close);
		closeAllAction = ActionFactory.CLOSE_ALL.create(window);
		closeAllAction.setText(M.CloseAll);

		// preferences
		preferencesAction = ActionFactory.PREFERENCES.create(window);
		preferencesAction.setImageDescriptor(Icon.PREFERENCES.descriptor());
		preferencesAction.setText(M.Settings);

		// import & export
		importAction = ActionFactory.IMPORT.create(window);
		importAction.setImageDescriptor(Icon.IMPORT.descriptor());
		importAction.setText(M.Import);
		exportAction = ActionFactory.EXPORT.create(window);
		exportAction.setImageDescriptor(Icon.EXPORT.descriptor());
		exportAction.setText(M.Export);

		// other
		exitAction = ActionFactory.QUIT.create(window);
		exitAction.setText(M.Exit);
		showViews = ContributionItemFactory.VIEWS_SHORTLIST.create(window);
		aboutAction = ActionFactory.ABOUT.create(window);
		aboutAction.setText(M.AboutOpenLCA);
	}

}
