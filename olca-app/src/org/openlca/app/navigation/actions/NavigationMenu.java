package org.openlca.app.navigation.actions;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.preferences.CloudPreference;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.cloud.CommitAction;
import org.openlca.app.navigation.actions.cloud.ConfigureRepositoriesAction;
import org.openlca.app.navigation.actions.cloud.FetchAction;
import org.openlca.app.navigation.actions.cloud.OpenCompareViewAction;
import org.openlca.app.navigation.actions.cloud.RebuildIndexAction;
import org.openlca.app.navigation.actions.cloud.ShowCommentsAction;
import org.openlca.app.navigation.actions.cloud.ShowInHistoryAction;
import org.openlca.app.navigation.actions.cloud.ToggleTrackingAction;
import org.openlca.app.navigation.actions.db.DbActivateAction;
import org.openlca.app.navigation.actions.db.DbAddLibraryAction;
import org.openlca.app.navigation.actions.db.DbCloseAction;
import org.openlca.app.navigation.actions.db.DbCopyAction;
import org.openlca.app.navigation.actions.db.DbCreateAction;
import org.openlca.app.navigation.actions.db.DbDeleteAction;
import org.openlca.app.navigation.actions.db.DbExportAction;
import org.openlca.app.navigation.actions.db.DbImportAction;
import org.openlca.app.navigation.actions.db.DbRenameAction;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.FileImport;
import org.openlca.app.util.Actions;
import org.openlca.app.viewers.Selections;
import org.slf4j.LoggerFactory;

/**
 * Adds the actions to the context menu of the navigation tree.
 */
public class NavigationMenu extends CommonActionProvider {

	private final INavigationAction[][] actions = new INavigationAction[][] {

			getDatabaseActions(),

			// model actions
			new INavigationAction[] {
					new OpenModelAction(),
					new CalculateSystemAction(),
					new CreateModelAction(),
					new OpenUsageAction(),
					new DeleteModelAction(),
					new DeleteLibraryAction(),
					ValidateAction.forModel()
			},

			// script actions
			new INavigationAction[] {
					new OpenScriptAction(),
					new DeleteScriptAction(),
					new ExportScriptAction(),
			},

			// transfer actions
			new INavigationAction[] {
					new CutAction(),
					new CopyAction(),
					new PasteAction()
			},

			// IO actions
			new INavigationAction[] {
					new ImportAction(),
					new ExportAction(),
			},

			// category actions
			new INavigationAction[] {
					new CreateCategoryAction(),
					new RenameAction(),
					new UseLibraryCategoryAction(),
			}
	};

	private final INavigationAction[][] cloudActions = new INavigationAction[][] {
			new INavigationAction[] {
					new CommitAction(),
					new FetchAction(),
					ToggleTrackingAction.untrack(),
					ToggleTrackingAction.track(),
					new ShowCommentsAction(),
					new ShowInHistoryAction()
			},
			new INavigationAction[] {
					new RebuildIndexAction(),
					new ConfigureRepositoriesAction()
			}
	};

	private final INavigationAction[][] cloudCompareActions = new INavigationAction[][] {
			new INavigationAction[] {
					new OpenCompareViewAction(false),
					new OpenCompareViewAction(true)
			}
	};

	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		var menu = actionBars.getMenuManager();
		if (menu == null)
			return;

		// when we add an action we need to check if it
		// was already added. we use the action title
		// as ID to identify an action for this.
		Consumer<Action> add = action -> {
			action.setId(action.getText());
			var existing = menu.find(action.getId());
			if (existing != null)
				return;
			menu.add(action);
		};

		var refresh = Actions.create(
				"Refresh",
				Icon.REFRESH.descriptor(),
				Navigator::refresh);
		add.accept(refresh);
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		ActionContext con = getContext();
		List<INavigationElement<?>> selection = Selections.allOf(
				con.getSelection());

		// create / import database
		if (showDbCreate(selection)) {
			menu.add(new DbCreateAction());
			menu.add(new DbImportAction());
			// TODO: not a nice hack here; better to have these tools
			// hooked somewhere else
			if (App.runsInDevMode() && Database.get() == null) {
				menu.add(new XNexusEcoinventIndexExportAction());
			}
		}

		// add dynamic actions
		if (selection.size() == 1) {
			registerSingleActions(selection.get(0), menu, actions);
		} else if (selection.size() > 1) {
			registerMultiActions(selection, menu, actions);
		}

		addImportMenu(selection, menu);
		addCloudMenu(selection, menu);
	}

	private boolean showDbCreate(List<INavigationElement<?>> elements) {
		if (elements.isEmpty())
			return true;
		if (elements.size() > 1)
			return false;
		INavigationElement<?> e = elements.get(0);
		return e instanceof DatabaseElement;
	}

	private void addCloudMenu(List<INavigationElement<?>> elements, IMenuManager menu) {
		if (!CloudPreference.doEnable())
			return;
		int registered = 0;
		IMenuManager subMenu = new MenuManager(M.Repository);
		if (elements.size() == 1)
			registered += registerSingleActions(elements.get(0), subMenu, cloudActions);
		else if (elements.size() > 1)
			registered += registerMultiActions(elements, subMenu, cloudActions);
		int subRegistered = 0;
		IMenuManager compareMenu = new MenuManager(M.CompareWith);
		if (elements.size() == 1)
			subRegistered += registerSingleActions(elements.get(0), compareMenu, cloudCompareActions);
		else if (elements.size() > 1)
			subRegistered += registerMultiActions(elements, compareMenu, cloudCompareActions);
		if (subRegistered > 0) {
			subMenu.add(compareMenu);
			registered++;
		}
		if (registered == 0)
			return;
		menu.add(subMenu);
		menu.add(new Separator());
	}

	private INavigationAction[] getDatabaseActions() {
		int count = App.runsInDevMode() ? 14 : 8;
		var actions = new INavigationAction[count];
		actions[0] = new DbExportAction();
		actions[1] = new DbActivateAction();
		actions[2] = ValidateAction.forDatabase();
		actions[3] = new DbCopyAction();
		actions[4] = new DbRenameAction();
		actions[5] = new DbDeleteAction();
		actions[6] = new DbCloseAction();
		actions[7] = new DbAddLibraryAction();
		if (App.runsInDevMode()) {
			actions[8] = new XEI3MetaDataImportAction();
			actions[9] = new XEI3MarketProcessCleanUp();
			actions[10] = new XNexusIndexExportAction();
			actions[11] = new XNexusEcoinventIndexExportAction();
			actions[12] = new XRefDataExport();
			actions[13] = new XRefDataImport();
		}
		return actions;
	}

	private int registerSingleActions(
			INavigationElement<?> element,
			IMenuManager menu,
			INavigationAction[][] actions) {
		int count = 0;
		for (INavigationAction[] group : actions) {
			boolean acceptedOne = false;
			for (INavigationAction action : group)
				if (action.accept(element)) {
					menu.add(action);
					count++;
					acceptedOne = true;
				}
			if (acceptedOne)
				menu.add(new Separator());
		}
		return count;
	}

	private int registerMultiActions(
			List<INavigationElement<?>> elements,
			IMenuManager menu,
			INavigationAction[][] actions) {
		int count = 0;
		for (INavigationAction[] group : actions) {
			boolean acceptedOne = false;
			for (INavigationAction action : group)
				if (action.accept(elements)) {
					menu.add(action);
					count++;
					acceptedOne = true;
				}
			if (acceptedOne)
				menu.add(new Separator());
		}
		return count;
	}

	public void addImportMenu(
			List<INavigationElement<?>> selection, IMenuManager menu) {
		if (selection.size() > 1)
			return;
		menu.add(new Separator());
		var icon = Icon.IMPORT.descriptor();
		var subMenu = new MenuManager(M.Import, icon, "import.menu");
		menu.add(subMenu);

		// try to determine the import from a file
		subMenu.add(Actions.create(
				M.File,
				Icon.FILE.descriptor(),
				() -> new FileImport().run()));

		// open the generic import dialog
		subMenu.add(Actions.create(M.Other + "...", icon, () -> {
			try {
				PlatformUI.getWorkbench()
						.getService(IHandlerService.class)
						.executeCommand(
								ActionFactory.IMPORT.getCommandId(),
								null);
			} catch (Exception e) {
				var log = LoggerFactory.getLogger(getClass());
				log.error("failed to open import dialog", e);
			}
		}));
	}
}
