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
	 * Copies the settings of this configuration to the
	 * given configuration.
	 */
	public void applyOn(GraphConfig other) {
		if (other == null)
			return;
		other.showFlowIcons = showFlowIcons;
		other.showFlowAmounts = showFlowAmounts;
		other.showElementaryFlows = showElementaryFlows;
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