package org.openlca.app.search;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.search.SearchQuery;
import org.openlca.app.collaboration.search.SearchView;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Popup;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchText extends WorkbenchWindowControlContribution {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private Text text;
	private DropDownAction action;

	@Override
	protected Control createControl(Composite parent) {
		parent.getParent().setRedraw(true); // fix tool-bar size on Windows
		parent.setLayout(new FillLayout());
		log.trace("create search text control");
		Composite composite = UI.composite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.horizontalSpacing = 5;
		layout.marginHeight = 0;
		layout.marginWidth = 5;
		layout.verticalSpacing = 0;
		composite.setLayout(layout);
		text = new Text(composite, SWT.BORDER | SWT.SEARCH);
		text.addTraverseListener(e -> {
			if (e.detail == SWT.TRAVERSE_RETURN) {
				if (action.searchOnline) {
					doSearchOnline();
				} else {
					doSearch(action.typeFilter);
				}
			}
		});
		UI.gridData(text, true, false).minimumWidth = 180;
		createActionMenu(composite);
		return composite;
	}

	private void createActionMenu(Composite composite) {
		ToolBar toolBar = new ToolBar(composite, SWT.NONE);
		ToolBarManager manager = new ToolBarManager(toolBar);
		action = new DropDownAction();
		manager.add(action);
		manager.update(true);
		toolBar.pack();
	}

	private void doSearch(ModelType typeFilter) {
		if (Database.get() == null) {
			Popup.info(M.NeedOpenDatabase);
			return;
		}
		String term = text.getText();
		if (typeFilter == ModelType.PARAMETER) {
			ParameterUsagePage.show(term);
			return;
		}
		var search = new Search(Database.get(), text.getText())
				.withTypeFilter(typeFilter);
		App.run(M.Searching, search,
				() -> SearchPage.show(term, search.getResult()));
	}

	private void doSearchOnline() {
		var query = new SearchQuery();
		query.query = text.getText();
		SearchView.open(query);
	}

	@SuppressWarnings("unused")
	private class SearchAction extends Action {
		public SearchAction() {
			setText(M.Search);
			setImageDescriptor(Icon.SEARCH.descriptor());
		}

		@Override
		public void run() {
			doSearch(null);
		}
	}

	/**
	 * A drop down menu for optionally selecting a model type as a search
	 * filter.
	 */
	private class DropDownAction extends Action implements IMenuCreator {

		private Menu menu;
		private ModelType typeFilter;
		private boolean searchOnline;

		public DropDownAction() {
			setText(M.Search);
			setImageDescriptor(Icon.SEARCH.descriptor());
			setMenuCreator(this);
		}

		@Override
		public void dispose() {
			if (menu == null)
				return;
			if (!menu.isDisposed())
				menu.dispose();
			menu = null;
		}

		@Override
		public Menu getMenu(Menu parent) {
			return null;
		}

		@Override
		public Menu getMenu(Control parent) {
			if (menu != null)
				menu.dispose();
			menu = new Menu(parent);
			ModelType[] types = {null,
					ModelType.PROJECT,
					ModelType.PRODUCT_SYSTEM,
					ModelType.PROCESS,
					ModelType.FLOW,
					ModelType.IMPACT_METHOD,
					ModelType.IMPACT_CATEGORY,
					ModelType.SOCIAL_INDICATOR,
					ModelType.PARAMETER,
					ModelType.FLOW_PROPERTY,
					ModelType.UNIT_GROUP,
					ModelType.CURRENCY,
					ModelType.ACTOR,
					ModelType.SOURCE,
					ModelType.LOCATION,
					ModelType.DQ_SYSTEM
			};
			for (ModelType type : types) {
				createItem(menu, getSearchLabel(type), type);
			}
			createSearchOnlineItem(menu);
			return menu;
		}

		private String getSearchLabel(ModelType type) {
			if (type == null)
				return M.SearchAllTypes;
			return switch (type) {
				case PROJECT -> M.SearchInProjects;
				case PRODUCT_SYSTEM -> M.SearchInProductSystems;
				case IMPACT_METHOD -> M.SearchInLCIAMethods;
				case IMPACT_CATEGORY -> M.SearchInLCIACategories;
				case PROCESS -> M.SearchInProcesses;
				case FLOW -> M.SearchInFlows;
				case SOCIAL_INDICATOR -> M.SearchInSocialIndicators;
				case PARAMETER -> M.SearchInParameters;
				case FLOW_PROPERTY -> M.SearchInFlowProperties;
				case UNIT_GROUP -> M.SearchInUnitGroups;
				case CURRENCY -> M.SearchInCurrencies;
				case ACTOR -> M.SearchInActors;
				case SOURCE -> M.SearchInSources;
				case LOCATION -> M.SearchInLocations;
				case DQ_SYSTEM -> M.SearchInDataQualitySystems;
				default -> M.Unknown;
			};
		}

		private void createItem(Menu menu, String text, ModelType type) {
			var image = type == null
					? Icon.SEARCH.get()
					: Images.get(type);
			var item = new MenuItem(menu, SWT.NONE);
			item.setText(text);
			item.setImage(image);
			Controls.onSelect(item, e -> updateSelection(text, type));
		}

		private void updateSelection(String text, ModelType type) {
			setText(text);
			var image = type == null
					? Icon.SEARCH.descriptor()
					: Images.descriptor(type);
			setImageDescriptor(image);
			typeFilter = type;
			searchOnline = false;
		}

		private void createSearchOnlineItem(Menu menu) {
			var item = new MenuItem(menu, SWT.NONE);
			item.setText("Search in Collaboration Server");
			item.setImage(Icon.COLLABORATION_SERVER_LOGO.get());
			Controls.onSelect(item, e -> onSearchOnlineSelection());
		}

		private void onSearchOnlineSelection() {
			setText("Search in Collaboration Server");
			setImageDescriptor(Icon.COLLABORATION_SERVER_LOGO.descriptor());
			typeFilter = null;
			searchOnline = true;
		}

		@Override
		public void run() {
			if (searchOnline) {
				doSearchOnline();
			} else {
				doSearch(typeFilter);
			}
		}
	}
}
