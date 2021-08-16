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
import org.openlca.app.navigation.actions.db.DbCloseAction;
import org.openlca.app.navigation.actions.db.DbCopyAction;
import org.openlca.app.navigation.actions.db.DbCreateAction;
import org.openlca.app.navigation.actions.db.DbDeleteAction;
import org.openlca.app.navigation.actions.db.DbExportAction;
import org.openlca.app.navigation.actions.db.DbRenameAction;
import org.openlca.app.navigation.actions.db.DbRestoreAction;
import org.openlca.app.navigation.actions.db.DbValidationAction;
import org.openlca.app.navigation.actions.libraries.DeleteLibraryAction;
import org.openlca.app.navigation.actions.libraries.ExportLibraryAction;
import org.openlca.app.navigation.actions.libraries.MountLibraryAction;
import org.openlca.app.navigation.actions.libraries.OpenLibraryAction;
import org.openlca.app.navigation.actions.libraries.UseLibraryCategoryAction;
import org.openlca.app.navigation.actions.scripts.DeleteScriptAction;
import org.openlca.app.navigation.actions.scripts.ExportScriptAction;
import org.openlca.app.navigation.actions.scripts.OpenScriptAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.FileImport;
import org.openlca.app.util.Actions;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.viewers.Selections;

/**
 * Adds the actions to the context menu of the navigation tree.
 */
public class NavigationMenu extends CommonActionProvider {

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

		// database actions
		addActions(selection, menu,
				new DbCreateAction(),
				new DbRestoreAction(),
				new DbExportAction(),
				new DbActivateAction(),
				new DbValidationAction(),
				new DbCopyAction(),
				new DbRenameAction(),
				new DbDeleteAction(),
				new DbCloseAction(),
				new MountLibraryAction());

		// dev. extensions
		if (App.runsInDevMode()) {
			addActions(selection, menu,
					new XEI3MetaDataImportAction(),
					new XEI3MarketProcessCleanUp(),
					new XNexusIndexExportAction(),
					new XNexusEcoinventIndexExportAction(),
					new XRefDataExport(),
					new XRefDataImport());
		}

		// model actions
		addActions(selection, menu,
				new OpenModelAction(),
				new CalculateSystemAction(),
				new CreateModelAction(),
				new OpenUsageAction(),
				new DeleteModelAction());

		// script & mapping actions
		addActions(selection, menu,
				new OpenScriptAction(),
				new OpenMappingAction(),
				new OpenLibraryAction(),
				new DeleteScriptAction(),
				new DeleteMappingAction(),
				new DeleteLibraryAction());

		// DnD actions
		addActions(selection, menu,
				new CutAction(),
				new CopyAction(),
				new PasteAction());

		// category actions
		addActions(selection, menu,
				new CreateCategoryAction(),
				new RenameAction(),
				new UseLibraryCategoryAction());

		addIOMenu(selection, menu);
		addCloudMenu(selection, menu);
	}

	private void addCloudMenu(
			List<INavigationElement<?>> selection, IMenuManager menu) {
		if (!CloudPreference.doEnable())
			return;

		// repo sub menu
		var subMenu = new MenuManager(M.Repository);
		int added = addActions(selection, menu,
				new CommitAction(),
				new FetchAction(),
				ToggleTrackingAction.untrack(),
				ToggleTrackingAction.track(),
				new ShowCommentsAction(),
				new ShowInHistoryAction());
		added += addActions(selection, menu,
				new RebuildIndexAction(),
				new ConfigureRepositoriesAction());

		// compare sub menuw
		var compareMenu = new MenuManager(M.CompareWith);
		int subAdded = addActions(selection, compareMenu,
				new OpenCompareViewAction(false),
				new OpenCompareViewAction(true));
		if (subAdded > 0) {
			subMenu.add(compareMenu);
			added++;
		}

		if (added == 0)
			return;
		menu.add(subMenu);
		menu.add(new Separator());
	}

	private int addActions(
			List<INavigationElement<?>> selection,
			IMenuManager menu,
			INavigationAction... actions) {
		int count = 0;
		for (var action : actions) {
			if (action.accept(selection)) {
				menu.add(action);
				count++;
			}
		}
		if (count > 1) {
			menu.add(new Separator());
		}
		return count;
	}

	private void addIOMenu(
			List<INavigationElement<?>> selection,
			IMenuManager menu) {
		menu.add(new Separator());
		MenuManager subMenu = createImportMenu();
		menu.add(subMenu);
		addActions(selection, menu,
				new ExportAction(),
				new ExportScriptAction(),
				new ExportFlowMapAction(),
				new ExportLibraryAction());
	}

	public static MenuManager createImportMenu() {
		var icon = Icon.IMPORT.descriptor();
		var menu = new MenuManager(
				M.Import, icon, "import.menu");

		// try to determine the import from a file
		menu.add(Actions.create(
				M.File,
				Icon.FILE.descriptor(),
				() -> new FileImport().run()));

		// open the generic import dialog
		menu.add(Actions.create(M.Other + "...", icon, () -> {
			try {
				PlatformUI.getWorkbench()
						.getService(IHandlerService.class)
						.executeCommand(
								ActionFactory.IMPORT.getCommandId(),
								null);
			} catch (Exception e) {
				ErrorReporter.on("failed to open import dialog", e);
			}
		}));
		return menu;
	}

}
