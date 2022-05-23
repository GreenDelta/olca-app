package org.openlca.app.editors.graph.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.M;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.editors.graph.themes.Theme;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;

public class NodeFigure extends Figure {

	final Node node;
	public final static Dimension HEADER_ARC_SIZE = new Dimension(15, 15);

	public NodeFigure(Node node) {
		this.node = node;
		var name = Labels.name(node.descriptor);

		setToolTip(new Label(name));
	}

	class NodeHeader extends Figure {

		NodeHeader() {
			var theme = node.getConfig().getTheme();
			var box = Theme.Box.of(node);

			GridLayout layout = new GridLayout(4, false);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			setLayoutManager(layout);

			var button1 = new ProcessExpanderButton();
			button1.setEnabled(true);
			add(button1, new GridData(SWT.LEAD, SWT.CENTER, false, true));

			add(new ImageFigure(Images.get(node.descriptor)), new GridData(SWT.LEAD, SWT.CENTER, false, true));

			var label = new Label(Labels.name(node.descriptor));
			label.setForegroundColor(theme.boxFontColor(box));
			add(label, new GridData(SWT.LEAD, SWT.CENTER, true, true));

			var button = new ProcessExpanderButton();
			button.setEnabled(true);
			add(button, new GridData(SWT.TRAIL, SWT.CENTER, false, true));

			setBackgroundColor(theme.boxBackgroundColor(box));
			setOpaque(true);
		}

	}

	public String toString() {
		var prefix = node.isMinimized() ? M.Minimize : M.Maximize;
		var name = Labels.name(node.descriptor);
		return "NodeFigure[" + prefix + "]("
			+ name.substring(0, Math.min(name.length(), 20)) + ")";
	}

}
