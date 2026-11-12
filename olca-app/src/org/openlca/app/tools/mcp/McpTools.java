package org.openlca.app.tools.mcp;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.database.IDatabase;

/// The registry of the tools that are available in the openLCA MCP server.
/// This is the place where tools can be filtered or configured later, e.g. to
/// only allow read-only tools.
public final class McpTools {

	/// General instructions that are sent to the agent when it connects to the
	/// server. They describe the data model and the typical workflow.
	public static final String INSTRUCTIONS = """
			This server gives access to an openLCA database for life cycle \
			assessment (LCA) modelling. Data sets are read and written in the \
			openLCA JSON-LD format (olca-schema, see \
			https://greendelta.github.io/olca-schema). Each data set has a @type \
			(e.g. Process, Flow, FlowProperty, UnitGroup, Location, \
			ProductSystem, ImpactMethod) and an @id (UUID). References to other \
			data sets are objects with @type and @id.

			Typical workflow to create a model:
			1. Search existing data with search_data before creating anything; \
			reuse existing flows, flow properties, unit groups, and locations.
			2. If needed, inspect a similar existing data set with get_data and \
			use it as a template.
			3. Create missing flows and processes with save_data. A process has \
			exchanges (inputs and outputs) that reference flows; set \
			quantitativeReference=true on the reference product output and \
			give each exchange an amount, a unit, and a flow property.
			4. Use get_providers to find providers for product inputs and set \
			them as defaultProvider of the input exchanges.
			5. Create a product system for the reference process with \
			create_product_system.

			Elementary flows (emissions, resources) usually exist already in \
			the database; search for them instead of creating new ones.""";

	private McpTools() {
	}

	/// Returns the tools of the server for the given database.
	public static List<McpTool> of(IDatabase db, DataSync sync) {
		var tools = new ArrayList<McpTool>();
		tools.addAll(DataTools.of(db, sync));
		return tools;
	}
}
