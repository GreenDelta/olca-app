package org.openlca.app.editors.graph.edit;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.swt.graphics.Color;
import org.openlca.app.editors.graph.model.IOPanel;

import java.beans.PropertyChangeEvent;
import java.util.Random;

public class IOPanelEditPart extends AbstractGraphEditPart<IOPanel> {

	@Override
	protected IFigure createFigure() {
		IFigure f = new RectangleFigure();
		f.setOpaque(true); // non-transparent figure
		Random r = new Random();
		f.setBackgroundColor(new Color(null, r.nextInt(256), r.nextInt(256), r.nextInt(256)));
		return f;
	}

	@Override
	protected void createEditPolicies() {

	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {

	}
}
