package org.openlca.app.tools.transfer;

public enum ProviderLinkingStrategy {

	PROCESS_UUID("Processes by identifier (UUID)"),
	PROCESS_NAME_AND_LOCATION("Processes by name and location");

	private final String label;

	ProviderLinkingStrategy(String label) {
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
