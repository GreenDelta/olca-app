package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.components.graphics.figures.ComponentFigure;
import org.openlca.app.components.graphics.model.Side;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;


public class NodeFigure extends ComponentFigure {

	public final Node node;
	public final static Dimension HEADER_ARC_SIZE = new Dimension(15, 15);
	public final PlusMinusButton inputExpandButton;
	public final PlusMinusButton outputExpandButton;
	private final String analysisGroup;

	public NodeFigure(Node node) {
		super(node);
		this.node = node;
		inputExpandButton = new PlusMinusButton(node, Side.INPUT);
		outputExpandButton = new PlusMinusButton(node, Side.OUTPUT);

		// check if the process is in an analysis group
		String group = null;
		var pid = node.descriptor.id;
		var sys = node.getGraph().getEditor().getProductSystem();
		for (var g : sys.analysisGroups) {
			if (g.processes.contains(pid)) {
				group = g.name;
				break;
			}
		}
		analysisGroup = group;
		setToolTip(new Label(name()));
	}

	protected String name() {
		return analysisGroup != null
				? analysisGroup + " :: " + Labels.name(node.descriptor)
				: Labels.name(node.descriptor);
	}

	class NodeHeader extends Figure {

		NodeHeader() {
			var theme = node.getGraph().getEditor().getTheme();
			var box = node.getThemeBox();

			GridLayout layout = new GridLayout(4, false);
			layout.marginHeight = 2;
			layout.marginWidth = 3;
			setLayoutManager(layout);

			add(inputExpandButton, new GridData(SWT.LEAD, SWT.CENTER, false, true));

			add(new ImageFigure(Images.get(node.descriptor)), new GridData(SWT.LEAD, SWT.CENTER, false, true));

			var label = new Label(NodeFigure.this.name());
			label.setForegroundColor(theme.boxFontColor(box));
			add(label, new GridData(SWT.LEAD, SWT.CENTER, true, true));

			add(outputExpandButton, new GridData(SWT.TRAIL, SWT.CENTER, false, true));

			setBackgroundColor(theme.boxBackgroundColor(box));
			setOpaque(true);
		}

	}

	public String toString() {
		return "Figure of " + node;
	}

}
