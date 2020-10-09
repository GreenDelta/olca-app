package org.openlca.app.editors.graphical;

public class GraphConfig {

	public boolean showFlowIcons;
	public boolean showFlowAmounts;
	public boolean showElementaryFlows;

	/**
	 * Creates a copy from the given configuration.
	 */
	public static GraphConfig from(GraphConfig other) {
		return other == null
				? new GraphConfig()
				: other.clone();
	}

	/**
	 * Set the values from the given configuration to
	 * this configuration.
	 */
	public void set(GraphConfig other) {
		if (other == null)
			return;
		showFlowIcons = other.showFlowIcons;
		showFlowAmounts = other.showFlowAmounts;
		showElementaryFlows = other.showElementaryFlows;
	}

	@Override
	protected GraphConfig clone() {
		var clone = new GraphConfig();
		clone.showFlowIcons = showFlowIcons;
		clone.showFlowAmounts = showFlowAmounts;
		clone.showElementaryFlows = showElementaryFlows;
		return clone;
	}
}