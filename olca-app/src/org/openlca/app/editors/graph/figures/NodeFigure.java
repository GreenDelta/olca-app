package org.openlca.app.editors.graph.figures;

import org.eclipse.draw2d.*;
import org.eclipse.swt.SWT;
import org.openlca.app.M;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.core.model.descriptors.RootDescriptor;

public class NodeFigure extends Figure {

	final Node node;
	final static public Integer ARC_SIZE = 15;

	public NodeFigure(Node node) {
		this.node = node;
	}

	class NodeHeader extends Figure {

		NodeHeader() {
			GridLayout layout = new GridLayout(4, false);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			setLayoutManager(layout);

			var button1 = new ProcessExpanderButton();
			button1.setEnabled(true);
			add(button1, new GridData(SWT.LEAD, SWT.CENTER, false, true));

			add(new ImageFigure(Images.get(node.descriptor)), new GridData(SWT.LEAD, SWT.CENTER, false, true));

			var label = new Label(Labels.name(node.descriptor));
			label.setForegroundColor(ColorConstants.black);
			add(label, new GridData(SWT.LEAD, SWT.CENTER, true, true));

			var button = new ProcessExpanderButton();
			button.setEnabled(true);
			add(button, new GridData(SWT.TRAIL, SWT.CENTER, false, true));
		}
		
	}

	public String toString() {
		var prefix = node.isMinimized() ? M.Minimize : M.Maximize;
		var name = Labels.name(node.descriptor);
		return "NodeFigure[" + prefix + "]("
			+ name.substring(0, Math.min(name.length(), 20)) + ")";
	}

}
