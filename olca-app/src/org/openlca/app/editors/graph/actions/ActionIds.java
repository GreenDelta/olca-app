package org.openlca.app.editors.graph.actions;

public interface ActionIds {

	String ADD_PROCESS = "graph.actions.AddProcessAction";
	String ADD_INPUT_EXCHANGE = "graph.actions.AddExchangeAction.Input";
	String ADD_OUTPUT_EXCHANGE = "graph.actions.AddExchangeAction.Output";

	String BUILD_NEXT_TIER = "graph.actions.BuildNextTierAction";
	String BUILD_SUPPLY_CHAIN = "graph.actions.BuildSupplyChainAction";
	String BUILD_SUPPLY_CHAIN_MENU = "graph.actions.BuildSupplyChainMenuAction";

	String MINIMIZE_ALL = "graph.actions.ChangeAllStateAction.MINIMIZE";
	String MAXIMIZE_ALL = "graph.actions.ChangeAllStateAction.MAXIMIZE";
	String EXPAND_ALL = "graph.actions.ExpansionAction.EXPAND";
	String COLLAPSE_ALL = "graph.actions.ExpansionAction.COLLAPSE";
	String LAYOUT_TREE = "graph.actions.LayoutAction.TREE_LAYOUT";
	String OPEN_MINIATURE_VIEW = "graph.actions.OpenMiniatureViewAction";
	String REMOVE_ALL_CONNECTIONS = "graph.actions.RemoveAllConnectionsAction";
	String REMOVE_SUPPLY_CHAIN = "graph.actions.RemoveSupplyChainAction";
	String SEARCH_PROVIDERS = "graph.actions.SearchConnectorsAction.PROVIDERS";
	String SEARCH_RECIPIENTS = "graph.actions.SearchConnectorsAction.RECIPIENTS";
	String SHOW_OUTLINE = "graph.actions.ShowOutlineAction";

}
