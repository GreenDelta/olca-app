package org.openlca.app.editors.graphical.view;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.command.MinMaxCommand;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.themes.Theme.Box;
import org.openlca.app.util.Labels;

import java.util.Timer;
import java.util.TimerTask;

public class NodeFigure extends Figure {

	public ProcessNode node;
	protected ProcessExpanderButton leftExpanderButton;
	protected ProcessExpanderButton rightExpanderButton;
	protected LineBorder border;

	public NodeFigure(ProcessNode node) {
		this.node = node;
	}

	public void setNode(ProcessNode node) {
		this.node = node;
	}

	public void refresh() {

		// refresh expanders
		if (leftExpanderButton != null) {
			leftExpanderButton.refresh();
		}
		if (rightExpanderButton != null) {
			rightExpanderButton.refresh();
		}
		// refresh the links of this node
		for (Link link : node.links) {
			if (node.equals(link.inputNode)) {
				link.refreshTargetAnchor();
			} else if (node.equals(link.outputNode)) {
				link.refreshSourceAnchor();
			}

		}
	}

	@Override
	protected void paintFigure(Graphics g) {
		var theme = node.config().theme();
		var box = Box.of(node);
		border.setColor(theme.boxBorderColor(box));
		g.pushState();
		g.setBackgroundColor(theme.boxBackgroundColor(box));
		g.fillRoundRectangle(new Rectangle(getLocation(), getSize()), 15, 15);
		g.popState();
		super.paintFigure(g);
	}

	static class Title extends Figure {

		private final ProcessNode node;
		private final Box box;
		private final Label label;

		Title(ProcessNode node) {
			this.node = node;
			this.box = Box.of(node);
			label = new Label(Labels.name(node.process));

			setToolTip(label);

			var layout = new GridLayout(1, true);
			layout.marginHeight = 2;
			layout.marginWidth = 10;
			setLayoutManager(layout);

			var theme = node.config().theme();
			label.setForegroundColor(theme.boxFontColor(box));

			add(label, new GridData(SWT.LEFT, SWT.TOP, true, false));
		}

		@Override
		protected void paintFigure(Graphics g) {
			var theme = node.config().theme();
			label.setForegroundColor(theme.boxFontColor(box));
			g.pushState();
			g.setBackgroundColor(theme.boxBackgroundColor(box));
			g.fillRectangle(new Rectangle(getLocation(), getSize()));
			g.popState();
			super.paintFigure(g);
		}
	}

}
