package org.openlca.app.rcp.images;

public enum Overlay {

	ADDED("overlay/cloud/added.png"), //
	ADD_TO_LOCAL("overlay/cloud/add_local.png"), //
	ADD_TO_REMOTE("overlay/cloud/add_remote.png"), //
	CONFLICT("overlay/cloud/conflict.png"), //
	DELETED("overlay/cloud/deleted.png"), //
	DELETE_FROM_LOCAL("overlay/cloud/delete_local.png"), //
	DELETE_FROM_REMOTE("overlay/cloud/delete_remote.png"), //
	MERGED("overlay/cloud/merged.png"), //
	MODIFY_IN_LOCAL("overlay/cloud/modify_local.png"), //
	MODIFY_IN_REMOTE("overlay/cloud/modify_remote.png"), //
	NEW("overlay/new.png");

	final String fileName;

	private Overlay(String fileName) {
		this.fileName = fileName;
	}

}
