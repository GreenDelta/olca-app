package org.openlca.app.editors.sd.editor.graph.model;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.components.graphics.model.Component;

/**
 * Represents a node in the system dynamics graph.
 * A node can be a Stock, Rate, or Auxiliary variable.
 */
public class SdNode extends Component {

	public static final String
			NAME_PROP = "name",
			TYPE_PROP = "type";

	private static final Dimension DEFAULT_SIZE = new Dimension(80, 60);
	private static final Point DEFAULT_LOCATION = new Point(50, 50);

	private final SdNodeType type;
	private String variableName;
	private String displayName;

	// TODO: Add reference to the actual SD variable from the model
	// private Var variable;

	public SdNode(SdNodeType type, String variableName) {
		this.type = type;
		this.variableName = variableName;
		this.displayName = variableName;
		setLocation(DEFAULT_LOCATION);
		setSize(getDefaultSize());
	}

	public SdNodeType getType() {
		return type;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String name) {
		String old = this.variableName;
		this.variableName = name;
		firePropertyChange(NAME_PROP, old, name);
	}

	public String getDisplayName() {
		return displayName != null ? displayName : variableName;
	}

	public void setDisplayName(String displayName) {
		String old = this.displayName;
		this.displayName = displayName;
		firePropertyChange(NAME_PROP, old, displayName);
	}

	/**
	 * Get the parent graph.
	 */
	public SdGraph getGraph() {
		var parent = getParent();
		if (parent instanceof SdGraph graph) {
			return graph;
		}
		return null;
	}

	/**
	 * Returns the default size based on node type.
	 */
	public Dimension getDefaultSize() {
		return switch (type) {
			case STOCK -> new Dimension(100, 60);
			case RATE -> new Dimension(50, 50);
			case AUXILIARY -> new Dimension(60, 30);
		};
	}

	@Override
	public int compareTo(Component other) {
		if (other instanceof SdNode node) {
			return this.variableName.compareTo(node.variableName);
		}
		return 0;
	}

	// TODO: Bind this node to an SD variable from the model
	// public void bindVariable(Var variable) {
	//     this.variable = variable;
	// }
	//
	// public Var getVariable() {
	//     return variable;
	// }
}
