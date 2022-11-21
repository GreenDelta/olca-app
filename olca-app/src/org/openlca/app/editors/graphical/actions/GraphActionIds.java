package org.openlca.app.editors.graphical.actions;

public interface GraphActionIds {

	String ADD_PROCESS = "graph.actions.AddProcessAction";
	String ADD_INPUT_EXCHANGE = "graph.actions.AddExchangeAction.Input";
	String ADD_OUTPUT_EXCHANGE = "graph.actions.AddExchangeAction.Output";
	String ADD_STICKY_NOTE = "graph.actions.AddStickyNoteAction";

	String BUILD_NEXT_TIER = "graph.actions.BuildNextTierAction";
	String BUILD_SUPPLY_CHAIN = "graph.actions.BuildSupplyChainAction";
	String BUILD_SUPPLY_CHAIN_MENU = "graph.actions.BuildSupplyChainMenuAction";

	String COLLAPSE_ALL = "graph.actions.ExpansionAction.COLLAPSE";
	String EXPAND_ALL = "graph.actions.ExpansionAction.EXPAND";

	String EDIT_EXCHANGE = "graph.actions.EditExchangeAction";
	String EDIT_STICKY_NOTE = "graph.actions.EditStickyNoteAction";
	String LAYOUT_TREE = "graph.actions.LayoutAction.TREE_LAYOUT";
	String LINK_UPDATE = "graph.actions.LinkUpdateAction";

	String MINIMIZE = "graph.actions.MinMaxAction.MINIMIZE";
	String MINIMIZE_ALL = "graph.actions.MinMaxAllAction.MINIMIZE";
	String MAXIMIZE = "graph.actions.MinMaxAction.MAXIMIZE";
	String MAXIMIZE_ALL = "graph.actions.MinMaxAllAction.MAXIMIZE";

	String REMOVE_ALL_CONNECTIONS = "graph.actions.RemoveAllConnectionsAction";
	String REMOVE_SUPPLY_CHAIN = "graph.actions.RemoveSupplyChainAction";
	String SEARCH_PROVIDERS = "graph.actions.SearchConnectorsAction.PROVIDERS";
	String SEARCH_RECIPIENTS = "graph.actions.SearchConnectorsAction.RECIPIENTS";
	String SET_REFERENCE = "graph.actions.SetReferenceAction";

}
