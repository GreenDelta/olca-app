package org.openlca.app.tools.mcp;

import org.openlca.core.model.descriptors.RootDescriptor;

/// Synchronizes data changes made by agents with the environment in which the
/// MCP server runs, e.g. the navigation and editors of the openLCA application.
/// Tools only use this interface and do not depend on the UI directly.
public interface DataSync {

	/// A synchronization that does nothing, e.g. for headless servers.
	DataSync NONE = new DataSync() {
	};

	/// Returns an error message if the given data set cannot be modified by an
	/// agent at the moment, e.g. when it is opened in an editor with unsaved
	/// changes. Returns `null` if the data set can be modified.
	default String checkModifiable(RootDescriptor d) {
		return null;
	}

	/// Called after the given data set was created or updated.
	default void saved(RootDescriptor d) {
	}

	/// Called after the given data set was deleted.
	default void deleted(RootDescriptor d) {
	}
}
