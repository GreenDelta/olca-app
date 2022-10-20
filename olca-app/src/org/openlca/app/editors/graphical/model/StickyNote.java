package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.tools.graphics.model.Component;
import org.openlca.util.Strings;

public class StickyNote extends MinMaxComponent {

	public static final Dimension DEFAULT_SIZE =
			new Dimension(250, SWT.DEFAULT);
	public String title;
	public String content = "";

	public void setTitle(String title) {
		this.title = title;
	}

	public void setContent(String content) {
		this.content = content;
	}

	@Override
	protected Dimension getMinimizedSize() {
		return new Dimension(getSize().width, DEFAULT_SIZE.height);
	}

	@Override
	protected Dimension getMaximizedSize() {
		return new Dimension(getSize().width, DEFAULT_SIZE.height);
	}

	@Override
	public void addChildren() {
	}

	public Graph getGraph() {
		return (Graph) getParent();
	}

	@Override
	public int compareTo(Component other) {
		if (other instanceof StickyNote note) {
			return Strings.compare(getComparisonLabel(), note.getComparisonLabel());
		}
		else return 0;
	}

	@Override
	public String getComparisonLabel() {
		return title;
	}

	public StickyNote copy() {
		var clone = new StickyNote();
		clone.setTitle(this.title);
		clone.setContent(this.content);
		clone.setSize(this.size);
		clone.setLocation(this.location);
		clone.setMinimized(this.isMinimized());
		return clone;
	}

}
