package org.openlca.app.editors.epds;

record ExportState(State state, String id) {

	enum State {
		/** Saved as local file. */
		FILE,

		/** Created new EPD on EC3. */
		CREATED,

		/** Updated existing EPD on EC3. */
		UPDATED,

		/** User canceled the export or there as an error. */
		CANCELED,
	}

	static ExportState canceled() {
		return new ExportState(State.CANCELED, null);
	}

	static ExportState created(String id) {
		return new ExportState(State.CREATED, id);
	}

	static ExportState updated(String id) {
		return new ExportState(State.UPDATED, id);
	}

	static ExportState file(String file) {
		return new ExportState(State.FILE, file);
	}

	boolean isCreated() {
		return state == State.CREATED && id != null;
	}

	public void display() {
		// Popup.info("Uploaded EPD", "The EPD was uploaded to " + loginPanel.url());
	}
}
