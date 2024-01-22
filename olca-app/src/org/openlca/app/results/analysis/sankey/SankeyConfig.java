package org.openlca.app.results.analysis.sankey;

import java.util.Objects;

import org.openlca.app.tools.graphics.model.Element;
import org.openlca.app.tools.graphics.themes.Theme;
import org.openlca.app.tools.graphics.themes.Themes;
import org.openlca.core.model.Copyable;
import org.openlca.util.Strings;

import static org.eclipse.draw2d.PositionConstants.NORTH;
import static org.openlca.app.tools.graphics.figures.Connection.ROUTER_CURVE;

public class SankeyConfig extends Element implements Copyable<SankeyConfig> {

	public static final String CONFIG_PROP = "config";

	private final SankeyEditor editor;

	private Object selection;
	private double cutoff = 0.0;
	private int maxCount = 25;
	private int orientation = NORTH;
	private String connectionRouter = ROUTER_CURVE;
	private Theme theme = Themes.getDefault(Themes.SANKEY);

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
					.min((i1, i2) -> Strings.compare(i1.name, i2.name))
					.orElse(null);
		}

		if (selection == null)
			selection = editor.items.enviFlows()
					.stream()
					.min((f1, f2) -> {
						if (f1.flow() == null || f2.flow() == null)
							return 0;
						return Strings.compare(f1.flow().name, f2.flow().name);
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
		clone.theme = theme;
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
		other.theme = theme;
		other.connectionRouter = connectionRouter;
		other.firePropertyChange(CONFIG_PROP, null, this);
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

	public Theme getTheme() {
		return theme;
	}

	public void setTheme(Theme theme) {
		if (theme != null) {
			this.theme = theme;
			firePropertyChange(CONFIG_PROP, null, this);
		}
	}

	public void setSelection(Object selection) {
		if (Objects.equals(this.selection, selection))
			return;
		this.selection = selection;
		firePropertyChange(CONFIG_PROP, null, this);
	}

	public void setCutoff(double cutoff) {
		if (cutoff == this.cutoff)
			return;
		this.cutoff = cutoff;
		firePropertyChange(CONFIG_PROP, null, this);
	}

	public void setMaxCount(int maxCount) {
		if (maxCount == this.maxCount)
			return;
		this.maxCount = maxCount;
		firePropertyChange(CONFIG_PROP, null, this);
	}

	public void setOrientation(int orientation) {
		if (orientation == this.orientation)
			return;
		this.orientation = orientation;
		firePropertyChange(CONFIG_PROP, null, this);
	}

	public void setConnectionRouter(String connectionRouter) {
		if (Objects.equals(connectionRouter, this.connectionRouter))
			return;
		this.connectionRouter = connectionRouter;
		firePropertyChange(CONFIG_PROP, null, this);
	}

}
