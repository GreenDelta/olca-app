package org.openlca.app.results.analysis.sankey;

import org.openlca.app.results.analysis.sankey.themes.Theme;
import org.openlca.app.results.analysis.sankey.themes.Themes;
import org.openlca.app.tools.graphics.model.Element;
import org.openlca.core.model.Copyable;
import org.openlca.util.Strings;

import static org.eclipse.draw2d.PositionConstants.NORTH;

public class SankeyConfig extends Element implements Copyable<SankeyConfig> {

	public static final String CONFIG_PROP = "config";
	public static final String ROUTER_NULL = "Straight line";
	public static final String ROUTER_CURVE = "Curve";
	public static final String ROUTER_MANHATTAN = "Manhattan";

	private final SankeyEditor editor;

	private Object selection;
	private double cutoff = 0.0;
	private int maxCount = 25;
	private int orientation = NORTH;
	private String connectionRouter = ROUTER_CURVE;
	private Theme theme = Themes.getDefault();

	public SankeyConfig(SankeyEditor editor) {
		this.editor = editor;
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

		if (editor.result.hasImpacts())
			selection = editor.items.impacts()
					.stream()
					.min((i1, i2) -> Strings.compare(i1.name, i2.name))
					.orElse(null);

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
			firePropertyChange(CONFIG_PROP, null, theme);
		}
	}

	public void setSelection(Object selection) {
		if (selection == this.selection)
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

}
