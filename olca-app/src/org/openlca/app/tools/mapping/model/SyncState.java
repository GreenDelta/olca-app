package org.openlca.app.tools.mapping.model;

/**
 * SyncState describes the state a mapping entry can have when synced with a
 * database.
 */
public enum SyncState {

	/**
	 * Indicates that the source flow of the mapping entry was not found.
	 */
	UNFOUND_SOURCE,

	/**
	 * Indicates that the target flow of the mapping entry was not found.
	 */
	UNFOUND_TARGET,

	/**
	 * Indicates that the source flow does not contain valid information (e.g. the
	 * flow in the database does not have a unit that was specified in the mapping).
	 */
	INVALID_SOURCE,

	/**
	 * Indicates that the target flow does not contain valid information.
	 */
	INVALID_TARGET,

	/**
	 * Indicates that the source flow was already assigned in another mapping.
	 */
	DUPLICATE,

	MATCHED,

	APPLIED,
}