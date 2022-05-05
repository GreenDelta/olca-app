package org.openlca.app.editors.graphical.view;

import org.eclipse.draw2d.*;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.themes.Theme;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;

public class MaximizedNodeFigure extends NodeFigure {

	public MaximizedNodeFigure(ProcessNode node) {
		super(node);
		initializeFigure();
		createHeader();
	}

	private void initializeFigure() {
		setToolTip(new Label(Labels.name(node.process)));
		setForegroundColor(Colors.white());
		var layout = new GridLayout(1, true);
		layout.horizontalSpacing = 10;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayoutManager(layout);
	}

	private void createHeader() {

		var layout = new GridLayout(4, false);
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 0;
		layout.marginHeight = 2;
		layout.marginWidth = 0;

		var top = new Figure();
		top.setLayoutManager(layout);

		// left expander
		leftExpanderButton = new ProcessExpanderButton(node, ProcessNode.Side.INPUT);
		top.add(leftExpanderButton, GridPos.leftCenter());

		// process icon and header
		top.add(new ImageFigure(Images.get(node.process)), GridPos.leftCenter());
		top.add(new Title(node), GridPos.fillTop());

		// right expander
		rightExpanderButton = new ProcessExpanderButton(node, ProcessNode.Side.OUTPUT);
		top.add(rightExpanderButton, GridPos.rightCenter());

		add(top, new GridData(SWT.FILL, SWT.FILL, true, false));

		// box border
		var theme = node.config().theme();
		border = new RoundBorder(theme.boxBorderWidth(Theme.Box.of(node)));
		setBorder(border);
	}
}
