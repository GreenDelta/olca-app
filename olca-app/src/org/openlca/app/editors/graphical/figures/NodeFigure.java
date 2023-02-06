package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.tools.graphics.figures.RoundBorder;
import org.openlca.app.tools.graphics.figures.SVGImage;
import org.openlca.app.tools.graphics.themes.Theme;
import org.openlca.app.tools.graphics.figures.ComponentFigure;
import org.openlca.app.util.Labels;

import static org.openlca.app.tools.graphics.model.Side.INPUT;
import static org.openlca.app.tools.graphics.model.Side.OUTPUT;

public class NodeFigure extends ComponentFigure {

	public final Node node;
	public final static Dimension HEADER_ARC_SIZE = new Dimension(15, 15);
	public final PlusMinusButton inputExpandButton;
	public final PlusMinusButton outputExpandButton;
	public NodeHeader header;

	public NodeFigure(Node node) {
		super(node);
		this.node = node;
		inputExpandButton = new PlusMinusButton(node, INPUT);
		outputExpandButton = new PlusMinusButton(node, OUTPUT);

		var name = Labels.name(node.descriptor);

		setToolTip(new Label(name));
	}

	public void initHeader() {
		header.init();
	}

	class NodeHeader extends Figure {

		private final Label label;

		NodeHeader() {
			var theme = node.getGraph().getConfig().getTheme();
			var box = Theme.Box.of(node.descriptor, node.isOfReferenceProcess());

			GridLayout layout = new GridLayout(4, false);
			layout.marginHeight = 2;
			layout.marginWidth = 3;
			setLayoutManager(layout);

			label = new Label(Labels.name(node.descriptor));
			label.setForegroundColor(theme.boxFontColor(box));

			setBackgroundColor(theme.boxBackgroundColor(box));
			setOpaque(true);
		}

		public void init() {
			add(inputExpandButton, new GridData(SWT.LEAD, SWT.CENTER, false, true));

			var renderedImage = Images.getSVG(node.descriptor);
			if (renderedImage != null) {
				var svg = new SVGImage(renderedImage, true, true, true);
				var height = getPreferredSize().height();
				add(svg, new GridData(SWT.LEAD, SWT.CENTER, false, true));
				// After testing, it is necessary to remove the border size from the
				// preferred image size.
				var size = height - 2 * ((RoundBorder) getBorder()).getWidth();
				svg.setPreferredImageSize(size, size);
			}

			add(label, new GridData(SWT.LEAD, SWT.CENTER, true, true));
			add(outputExpandButton, new GridData(SWT.TRAIL, SWT.CENTER, false, true));
		}

	}

	public String toString() {
		return "Figure of " + node;
	}

}
