package org.openlca.app.tools.transfer;

enum LinkingStrategy {

	BY_ID(),
	BY_NAME("");

	private final String label;

	LinkingStrategy(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	@Override
	public String toString() {
		return label;
	}
}
