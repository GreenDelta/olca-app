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
import org.openlca.app.collaboration.navigation.actions.CloneFromGitAction;
import org.openlca.app.collaboration.navigation.actions.RepositoryMenu;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.db.DbActivateAction;
import org.openlca.app.navigation.actions.db.DbCategoryAction;
import org.openlca.app.navigation.actions.db.DbCloseAction;
import org.openlca.app.navigation.actions.db.DbCopyAction;
import org.openlca.app.navigation.actions.db.DbCreateAction;
import org.openlca.app.navigation.actions.db.DbDeleteAction;
import org.openlca.app.navigation.actions.db.DbExportAction;
import org.openlca.app.navigation.actions.db.DbRenameAction;
import org.openlca.app.navigation.actions.db.DbRestoreAction;
import org.openlca.app.navigation.actions.db.DbValidationAction;
import org.openlca.app.navigation.actions.libraries.AddLibraryAction;
import org.openlca.app.navigation.actions.libraries.DeleteLibraryAction;
import org.openlca.app.navigation.actions.libraries.ExportLibraryAction;
import org.openlca.app.navigation.actions.libraries.OpenLibraryAction;
import org.openlca.app.navigation.actions.libraries.UpdateLibraryAction;
import org.openlca.app.navigation.actions.nexus.XNexusCsvIndexExportAction;
import org.openlca.app.navigation.actions.nexus.XNexusEcoinventIndexExportAction;
import org.openlca.app.navigation.actions.nexus.XNexusIndexExportAction;
import org.openlca.app.navigation.actions.scripts.DeleteScriptAction;
import org.openlca.app.navigation.actions.scripts.ExportScriptAction;
import org.openlca.app.navigation.actions.scripts.OpenScriptAction;
import org.openlca.app.navigation.elements.DatabaseElement;
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
				M.Refresh,
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
		addNewDbActions(selection, menu,
				new DbRestoreAction(),
				new DbCreateAction(),
				new CloneFromGitAction());
		addActions(selection, menu,
				new DbExportAction(),
				new DbActivateAction(),
				new DbValidationAction(),
				new DbCopyAction(),
				new DbRenameAction(),
				new DbCategoryAction(),
				new DbDeleteAction(),
				new DbCloseAction(),
				new AddLibraryAction());

		// dev. extensions
		if (App.runsInDevMode()) {
			addActions(selection, menu,
					new XEI3MetaDataImportAction(),
					new XEI3MarketProcessCleanUp(),
					new XNexusCsvIndexExportAction(),
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
				new DeleteLibraryAction(),
				new UpdateLibraryAction());

		// DnD actions
		addActions(selection, menu,
				new CutAction(),
				new CopyAction(),
				new PasteAction());

		// category actions
		addActions(selection, menu,
				new CreateCategoryAction(),
				new RenameAction());

		addIOMenu(selection, menu);
		RepositoryMenu.add(selection, menu);
	}

	public static int addActions(
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
		if (selection.size() == 1
				&& selection.get(0) instanceof DatabaseElement dbElem
				&& Database.isActive(dbElem.getContent())) {
			var subMenu = createImportMenu();
			menu.add(subMenu);
		}
		addActions(selection, menu,
				new ExportAction(),
				new ExportScriptAction(),
				new ExportFlowMapAction(),
				new ExportLibraryAction());
	}

	private static void addNewDbActions(
			List<INavigationElement<?>> selection,
			IMenuManager menu,
			INavigationAction... actions) {
		var newDbMenu = new MenuManager(M.NewDatabase);
		newDbMenu.setImageDescriptor(Icon.DATABASE.descriptor());
		addActions(selection, newDbMenu, actions);
		menu.add(newDbMenu);
	}

	public static MenuManager createImportMenu() {
		var icon = Icon.IMPORT.descriptor();
		var menu = new MenuManager(
				M.ImportDots, icon, "import.menu");

		// try to determine the import from a file
		menu.add(Actions.create(
				M.AmpFile,
				Icon.FILE.descriptor(),
				() -> new FileImport().run()));

		// open the generic import dialog
		menu.add(Actions.create(M.OtherDots, icon, () -> {
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
