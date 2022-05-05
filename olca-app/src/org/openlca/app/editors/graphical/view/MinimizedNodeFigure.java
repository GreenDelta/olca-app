package org.openlca.app.editors.graphical.view;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.themes.Theme;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;

public class MinimizedNodeFigure extends NodeFigure {

	public MinimizedNodeFigure(ProcessNode node) {
		super(node);

		var layout = new GridLayout(4, false);
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 0;
		layout.marginHeight = 2;
		layout.marginWidth = 0;

		setLayoutManager(layout);

		// left expander
		leftExpanderButton = new ProcessExpanderButton(node, ProcessNode.Side.INPUT);
		add(leftExpanderButton, GridPos.leftCenter());

		// process icon and header
		add(new ImageFigure(Images.get(node.process)), GridPos.leftCenter());
		add(new Title(node), GridPos.fillTop());

		// right expander
		rightExpanderButton = new ProcessExpanderButton(node, ProcessNode.Side.OUTPUT);
		add(rightExpanderButton, GridPos.rightCenter());

		// box border
		var theme = node.config().theme();
		border = new RoundBorder(theme.boxBorderWidth(Theme.Box.of(node)));
		setBorder(border);
	}
}
