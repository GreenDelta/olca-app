package org.openlca.app.tools.graphics.figures;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.swt.graphics.Color;

import static org.eclipse.swt.SWT.ON;

public class SelectableConnection extends PolylineConnection {

	private final Color color;
	private final Color colorSelected;
	private boolean selected;

	public SelectableConnection(Color color, Color colorSelected) {
		this.color = color;
		this.colorSelected = colorSelected;
	}

	@Override
	public void paint(Graphics g) {
		setAntialias(ON);
		setForegroundColor(selected ? colorSelected : color);
		super.paint(g);
	}

	/**
	 * Sets the selection state of this Connection
	 *
	 * @param b
	 *            true will cause the figure to appear selected
	 */
	public void setSelected(boolean b) {
		selected = b;
		repaint();
	}

	/**
	 * Return the selection state of this connection.
	 */
	public boolean isSelected() {
		return selected;
	}

}
