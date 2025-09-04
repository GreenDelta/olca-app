package org.openlca.app.rcp;

import java.io.File;
import java.util.Objects;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
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
import org.openlca.app.App;
import org.openlca.app.AppContext;
import org.openlca.app.Config;
import org.openlca.app.M;
import org.openlca.app.collaboration.browse.ServerNavigator;
import org.openlca.app.components.FileChooser;
import org.openlca.app.components.replace.ReplaceFlowsDialog;
import org.openlca.app.components.replace.ReplaceProvidersDialog;
import org.openlca.app.db.Database;
import org.openlca.app.devtools.IpcDialog;
import org.openlca.app.devtools.agent.AgentEditor;
import org.openlca.app.devtools.python.PythonEditor;
import org.openlca.app.devtools.sql.SqlEditor;
import org.openlca.app.editors.StartPage;
import org.openlca.app.logging.Console;
import org.openlca.app.logging.LogFileEditor;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.ExportAction;
import org.openlca.app.navigation.actions.NavigationMenu;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.tools.hestia.HestiaTool;
import org.openlca.app.tools.libraries.LibraryExportDialog;
import org.openlca.app.tools.mapping.MappingTool;
import org.openlca.app.tools.openepd.EpdPanel;
import org.openlca.app.tools.params.ParameterAnalysisDialog;
import org.openlca.app.tools.smartepd.SmartEpdTool;
import org.openlca.app.tools.soda.SodaClientTool;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.MsgBox;
import org.openlca.core.model.ModelType;
import org.openlca.io.ecospold2.input.EcoSpold2Import;
import org.openlca.io.ecospold2.input.ImportConfig;
import org.openlca.io.ecospold2.input.MethodImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class RcpActionBarAdvisor extends ActionBarAdvisor {

	private IWorkbenchAction aboutAction;
	private IWorkbenchAction closeAction;
	private IWorkbenchAction closeAllAction;
	private IWorkbenchAction exitAction;
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
		var toolbar = new ToolBarManager(SWT.FLAT | SWT.LEFT);
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
		fillToolsMenu(menuBar);
		fillHelpMenu(menuBar);
		removeActionSets();
	}

	private void removeActionSets() {
		// currently we just remove the cheat-sheets here; see:
		// http://random-eclipse-tips.blogspot.de/2009/02/eclipse-rcp-removing-unwanted_02.html
		var reg = WorkbenchPlugin.getDefault().getActionSetRegistry();
		for (var aset : reg.getActionSets()) {
			if (Objects.equals(aset.getId(),
				"org.eclipse.ui.cheatsheets.actionSet")) {
				var ext = aset.getConfigurationElement()
					.getDeclaringExtension();
				reg.removeExtension(ext, new Object[]{aset});
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
			M.AmpFile, IWorkbenchActionConstants.M_FILE);
		menu.add(saveAction);
		menu.add(saveAsAction);
		menu.add(saveAllAction);
		menu.add(new Separator());
		menu.add(closeAction);
		menu.add(closeAllAction);
		menu.add(new Separator());
		menu.add(preferencesAction);

		// import and export
		menu.add(new Separator());
		var impMenu = NavigationMenu.createImportMenu();
		menu.add(impMenu);
		menu.add(new ExportAction());
		menu.add(new Separator());
		menu.add(exitAction);
		menuBar.add(menu);
	}

	private void fillToolsMenu(IMenuManager menuBar) {
		var menu = new MenuManager(M.Tools);
		var viewMenu = new MenuManager(M.ShowViews);
		viewMenu.add(showViews);
		menu.add(viewMenu);
		menu.add(new Separator());

		createDeveloperMenu(menu);

		// bulk replace
		var brMenu = new MenuManager(M.Bulkreplace);
		menu.add(brMenu);
		brMenu.add(Actions.create(
			M.Flows, Images.descriptor(ModelType.FLOW),
			ReplaceFlowsDialog::openDialog));
		brMenu.add(Actions.create(
			M.Providers, Images.descriptor(ModelType.PROCESS),
			ReplaceProvidersDialog::openDialog));

		// flow mapping
		var mappings = new MenuManager(M.FlowMappingExperimental);
		menu.add(mappings);
		mappings.add(Actions.create(M.New, MappingTool::createNew));
		mappings.add(Actions.create(M.OpenFile, MappingTool::openFile));

		// library export
		menu.add(Actions.create(
				M.LibraryExportExperimental, LibraryExportDialog::show));

		menu.add(Actions.create(
				"Parameter analysis (experimental)", ParameterAnalysisDialog::show));

		// API clients
		var apiMenu = new MenuManager("API Clients");
		menu.add(apiMenu);
		apiMenu.add(Actions.create("CS Servers",
				Icon.COLLABORATION_SERVER_LOGO.descriptor(), ServerNavigator::open));
		apiMenu.add(Actions.create("soda4LCA",
				Icon.SODA.descriptor(), SodaClientTool::open));
		apiMenu.add(Actions.create(M.GetEpdsFromEc3,
				Icon.BUILDING.descriptor(), EpdPanel::open));
		apiMenu.add(Actions.create("SmartEPD (experimental)",
				Icon.SMART_EPD.descriptor(), SmartEpdTool::open));
		apiMenu.add(Actions.create("Hestia (experimental)",
				Icon.HESTIA.descriptor(), HestiaTool::open));

		// console
		menu.add(new Separator());
		menu.add(new FormulaConsoleAction());

		// add tools actions for ecoinvent imports
		if (App.runsInDevMode()) {
			menu.add(new Separator());
			MenuManager eiMenu = new MenuManager("ecoinvent 3.x");
			menu.add(eiMenu);
			eiMenu.add(Actions.create(M.ImportProcesses,
				() -> runSpold2Import(ModelType.PROCESS)));
			eiMenu.add(Actions.create(M.ImportLciaMethods,
				() -> runSpold2Import(ModelType.IMPACT_METHOD)));
		}
		menuBar.add(menu);
	}

	private void runSpold2Import(ModelType type) {
		var db = Database.get();
		if (db == null) {
			MsgBox.error(M.NoDatabaseOpened, M.NoDatabaseOpenedErr);
			return;
		}
		File file = FileChooser.open("*.zip");
		if (file == null)
			return;
		File[] files = new File[]{file};

		Runnable imp = null;
		if (type == ModelType.IMPACT_METHOD) {
			imp = new MethodImport(files, db);
		} else if (type == ModelType.PROCESS) {
			ImportConfig conf = new ImportConfig(db);
			conf.checkFormulas = true;
			conf.skipNullExchanges = true;
			conf.withParameterFormulas = false;
			conf.withParameters = false;
			EcoSpold2Import imp_ = new EcoSpold2Import(conf);
			imp_.setFiles(files);
			imp = imp_;
		}
		if (imp == null)
			return;

		try {
			App.runWithProgress(M.RunImport, imp);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("EcoSpold 2 import failed", e);
		} finally {
			Navigator.refresh();
			AppContext.evictAll();
		}
	}

	private void createDeveloperMenu(MenuManager menu) {
		var devMenu = new MenuManager(M.DeveloperTools);
		menu.add(devMenu);
		devMenu.add(Actions.create("SQL", Icon.SQL.descriptor(), SqlEditor::open));
		devMenu.add(Actions.create(M.Console, Icon.CONSOLE.descriptor(), Console::show));
		devMenu.add(Actions.create(M.Python, Icon.PYTHON.descriptor(), PythonEditor::open));
		devMenu.add(Actions.create("Agent", Icon.PYTHON.descriptor(), AgentEditor::open));
		devMenu.add(Actions.create(M.IpcServer, Icon.IPC.descriptor(), IpcDialog::show));
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
		saveAsAction.setText(M.SaveAsDots);
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
		preferencesAction.setText(M.Preferences);

		// other
		exitAction = ActionFactory.QUIT.create(window);
		exitAction.setText(M.Exit);
		showViews = ContributionItemFactory.VIEWS_SHORTLIST.create(window);
		aboutAction = ActionFactory.ABOUT.create(window);
		aboutAction.setText(M.AboutOpenLCA);
	}

}
