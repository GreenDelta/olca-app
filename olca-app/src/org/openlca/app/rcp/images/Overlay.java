package org.openlca.app.rcp.images;

public enum Overlay {

	ADDED("overlay/collaboration/added.png"),
	ADD_TO_LOCAL("overlay/collaboration/add_local.png"),
	ADD_TO_REMOTE("overlay/collaboration/add_remote.png"),
	CONFLICT("overlay/collaboration/conflict.png"),
	DELETED("overlay/collaboration/deleted.png"),
	DELETE_FROM_LOCAL("overlay/collaboration/delete_local.png"),
	DELETE_FROM_REMOTE("overlay/collaboration/delete_remote.png"),
	INVALID("overlay/red_dot.png"),
	LIBRARY("overlay/library.png"),
	MERGED("overlay/collaboration/merged.png"),
	MODIFY_IN_LOCAL("overlay/collaboration/modify_local.png"),
	MODIFY_IN_REMOTE("overlay/collaboration/modify_remote.png"),
	NEW("overlay/new.png"),
	VALID("overlay/green_dot.png");

	final String fileName;

	Overlay(String fileName) {
		this.fileName = fileName;
	}

}
