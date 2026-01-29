package org.openlca.app.results.analysis.sankey;

import static org.eclipse.draw2d.PositionConstants.*;
import static org.openlca.app.components.graphics.figures.Connection.*;

import java.util.Objects;

import org.openlca.app.components.graphics.model.PropertyNotifier;
import org.openlca.commons.Copyable;
import org.openlca.commons.Strings;

public class SankeyConfig extends PropertyNotifier implements Copyable<SankeyConfig> {

	public static final String CONFIG_PROP = "config";

	private final SankeyEditor editor;

	private Object selection;
	private double cutoff = 0.0;
	private int maxCount;
	private int orientation = NORTH;
	private String connectionRouter = ROUTER_CURVE;

	public SankeyConfig(SankeyEditor editor) {
		this.editor = editor;
		var numberOfTechFlow = editor.items.techFlows().size();
		maxCount = Math.min(numberOfTechFlow, 25);
		initSelection();
	}

	/**
	 * Creates a copy from the given configuration.
	 */
	public static SankeyConfig from(SankeyConfig other, SankeyEditor editor) {
		return other == null
				? new SankeyConfig(editor)
				: other.copy();
	}

	private void initSelection() {
		if (editor.result == null)
			return;

		if (editor.result.hasImpacts()) {
			selection = editor.items.impacts()
					.stream()
					.min((i1, i2) -> Strings.compareIgnoreCase(i1.name, i2.name))
					.orElse(null);
		}

		if (selection == null)
			selection = editor.items.enviFlows()
					.stream()
					.min((f1, f2) -> {
						if (f1.flow() == null || f2.flow() == null)
							return 0;
						return Strings.compareIgnoreCase(f1.flow().name, f2.flow().name);
					})
					.orElse(null);
		// TODO costs...
	}

	@Override
	public SankeyConfig copy() {
		var clone = new SankeyConfig(editor);
		clone.selection = selection;
		clone.cutoff = cutoff;
		clone.maxCount = maxCount;
		clone.orientation = orientation;
		clone.connectionRouter = connectionRouter;
		return clone;
	}

	/**
	 * Copies the settings of this configuration to the
	 * given configuration.
	 */
	public void copyTo(SankeyConfig other) {
		if (other == null)
			return;
		other.selection = selection;
		other.cutoff = cutoff;
		other.maxCount = maxCount;
		other.orientation = orientation;
		other.connectionRouter = connectionRouter;
		other.notifyChange(CONFIG_PROP, null, this);
	}

	public String connectionRouter() {
		return connectionRouter;
	}

	public Object selection() {
		return selection;
	}

	public double cutoff() {
		return cutoff;
	}

	public int maxCount() {
		return maxCount;
	}

	public int orientation() {
		return orientation;
	}

	public void setSelection(Object selection) {
		if (Objects.equals(this.selection, selection))
			return;
		this.selection = selection;
		notifyChange(CONFIG_PROP, null, this);
	}

	public void setCutoff(double cutoff) {
		if (cutoff == this.cutoff)
			return;
		this.cutoff = cutoff;
		notifyChange(CONFIG_PROP, null, this);
	}

	public void setMaxCount(int maxCount) {
		if (maxCount == this.maxCount)
			return;
		this.maxCount = maxCount;
		notifyChange(CONFIG_PROP, null, this);
	}

	public void setOrientation(int orientation) {
		if (orientation == this.orientation)
			return;
		this.orientation = orientation;
		notifyChange(CONFIG_PROP, null, this);
	}

	public void setConnectionRouter(String connectionRouter) {
		if (Objects.equals(connectionRouter, this.connectionRouter))
			return;
		this.connectionRouter = connectionRouter;
		notifyChange(CONFIG_PROP, null, this);
	}

}
