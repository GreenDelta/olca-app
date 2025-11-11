package org.openlca.app.editors.graphical.figures;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.openlca.app.components.graphics.figures.ComponentFigure;
import org.openlca.app.components.graphics.model.Side;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.commons.Strings;
import org.openlca.core.model.AnalysisGroup;


public class NodeFigure extends ComponentFigure {

	public final Node node;
	public final static Dimension HEADER_ARC_SIZE = new Dimension(15, 15);
	public final PlusMinusButton inputExpandButton;
	public final PlusMinusButton outputExpandButton;
	protected AnalysisGroup analysisGroup;
	private final List<Runnable> analysisGroupListeners = new ArrayList<>();

	public NodeFigure(Node node) {
		super(node);
		this.node = node;
		inputExpandButton = new PlusMinusButton(node, Side.INPUT);
		outputExpandButton = new PlusMinusButton(node, Side.OUTPUT);

		// check if the process is in an analysis group
		var pid = node.descriptor.id;
		var sys = node.getGraph().getEditor().getProductSystem();
		for (var g : sys.analysisGroups) {
			if (g.processes.contains(pid)) {
				analysisGroup = g;
				break;
			}
		}

		var toolTipLabel = new Label(name());
		setToolTip(toolTipLabel);
		onAnalysisGroupChange(() -> toolTipLabel.setText(name()));
	}

	protected String name() {
		return analysisGroup != null
				? analysisGroup.name + " :: " + Labels.name(node.descriptor)
				: Labels.name(node.descriptor);
	}

	protected Color borderColor() {
		if (analysisGroup != null && Strings.isNotBlank(analysisGroup.color))
			return Colors.fromHex(analysisGroup.color);
		var theme = node.getGraph().getEditor().getTheme();
		var box = node.getThemeBox();
		return theme.boxBorderColor(box);
	}

	protected void onAnalysisGroupChange(Runnable fn) {
		analysisGroupListeners.add(fn);
	}

	public void updateAnalysisGroup(AnalysisGroup group) {
		analysisGroup = group;
		for (var l : analysisGroupListeners) {
			l.run();
		}
	}


	class NodeHeader extends Figure {

		private final Label label;

		NodeHeader() {
			var theme = node.getGraph().getEditor().getTheme();
			var box = node.getThemeBox();

			GridLayout layout = new GridLayout(4, false);
			layout.marginHeight = 2;
			layout.marginWidth = 3;
			setLayoutManager(layout);

			add(inputExpandButton, new GridData(SWT.LEAD, SWT.CENTER, false, true));

			add(new ImageFigure(Images.get(node.descriptor)),
					new GridData(SWT.LEAD, SWT.CENTER, false, true));

			label = new Label(NodeFigure.this.name());
			label.setForegroundColor(theme.boxFontColor(box));
			add(label, new GridData(SWT.LEAD, SWT.CENTER, true, true));

			add(outputExpandButton, new GridData(SWT.TRAIL, SWT.CENTER, false, true));

			setBackgroundColor(theme.boxBackgroundColor(box));
			setOpaque(true);
		}

		Label getLabel() {
			return label;
		}
	}

	@Override
	public String toString() {
		return "Figure of " + node;
	}

}
